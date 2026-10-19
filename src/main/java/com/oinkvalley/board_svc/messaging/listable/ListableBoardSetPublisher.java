package com.oinkvalley.board_svc.messaging.listable;

import com.oinkvalley.board_svc.config.NatsConfig;
import com.oinkvalley.board_svc.db.domain.Board;
import com.oinkvalley.board_svc.db.domain.BoardPostReadPolicy;
import com.oinkvalley.board_svc.db.domain.BoardRolePermission;
import com.oinkvalley.board_svc.db.repository.BoardRepository;
import com.oinkvalley.board_svc.db.repository.BoardRolePermissionRepository;
import io.nats.client.KeyValue;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 역할별 "목록에 모아 보여도 되는 게시판(listable)" 집합을 계산해 NATS KV({@value #BUCKET})에 기록.
 * 규칙의 주인은 board — search-svc(둘러보기·검색)는 결과만 읽는다.
 * listable = 활성 + 글 읽기 정책 ROLE_READERS + 그 역할에 읽기 권한. 작성자 전용 게시판은 여러 게시판을 가로지르는 목록에 넣지 않는다.
 * key = 역할, value = {"version": epochMillis, "boards": [{"id","slug","name"}]}. 빈 목록도 기록(키 없음 = 모름 = 닫힘).
 *
 * 기록 시점: 시작 시, 게시판 생성·수정 커밋 후, 주기적으로(권한은 API 없이 SQL 마이그레이션으로 바뀌므로).
 * Redis 도입 시 저장소만 옮긴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ListableBoardSetPublisher {

    public static final String BUCKET = "LISTABLE_BOARDS";
    private static final JsonMapper JSON = JsonMapper.shared();

    private final BoardRepository boardRepository;
    private final BoardRolePermissionRepository permissionRepository;
    private final NatsConfig natsConfig;

    /** NATS 에 (재)연결될 때마다 다시 쓴다 — 기동 시 NATS 가 늦게 떠도 붙는 즉시 반영. */
    @PostConstruct
    void registerRefreshOnConnect() {
        natsConfig.onConnected(this::refresh);
    }

    @Scheduled(fixedDelayString = "${board.listable-boards.refresh-ms}", initialDelayString = "${board.listable-boards.refresh-ms}")
    public void periodic() {
        refresh();
    }

    /** 현재 트랜잭션 커밋 뒤에 다시 계산 (트랜잭션 밖이면 즉시). */
    public void refreshAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            refresh();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                refresh();
            }
        });
    }

    @Transactional(readOnly = true)
    public synchronized void refresh() {
        if (!natsConfig.isConnected()) {
            return;
        }
        try {
            Map<String, List<Map<String, Object>>> byRole = compute();
            KeyValue kv = natsConfig.keyValueBucket(BUCKET);
            long version = System.currentTimeMillis();
            for (Map.Entry<String, List<Map<String, Object>>> entry : byRole.entrySet()) {
                Map<String, Object> value = new LinkedHashMap<>();
                value.put("version", version);
                value.put("boards", entry.getValue());
                kv.put(entry.getKey(), JSON.writeValueAsBytes(value));
            }
            Set<String> stale = new HashSet<>(kv.keys());
            stale.removeAll(byRole.keySet());
            for (String role : stale) {
                kv.delete(role);
            }
            log.debug("listable boards refreshed roles={}", byRole.keySet());
        } catch (Exception e) {
            log.warn("listable boards refresh failed: {}", e.toString());
        }
    }

    private Map<String, List<Map<String, Object>>> compute() {
        Map<Long, Board> listableBoards = boardRepository.findAll().stream()
                .filter(Board::isActive)
                .filter(b -> b.getPostReadPolicy() == BoardPostReadPolicy.ROLE_READERS)
                .collect(Collectors.toMap(Board::getId, b -> b));

        Map<String, List<Map<String, Object>>> byRole = new TreeMap<>();
        for (BoardRolePermission permission : permissionRepository.findAll()) {
            List<Map<String, Object>> boards = byRole.computeIfAbsent(permission.getRole(), r -> new ArrayList<>());
            Board board = listableBoards.get(permission.getBoardId());
            if (permission.isCanRead() && board != null) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", board.getId());
                item.put("slug", board.getSlug());
                item.put("name", board.getName());
                boards.add(item);
            }
        }
        byRole.values().forEach(list -> list.sort((a, b) -> Long.compare((Long) a.get("id"), (Long) b.get("id"))));
        return byRole;
    }
}
