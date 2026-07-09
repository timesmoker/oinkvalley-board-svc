package com.oinkvalley.board_svc.service;

import com.oinkvalley.board_svc.db.domain.Board;
import com.oinkvalley.board_svc.db.domain.BoardPostReadPolicy;
import com.oinkvalley.board_svc.db.domain.BoardRolePermission;
import com.oinkvalley.board_svc.dto.board.BoardCreateRequest;
import com.oinkvalley.board_svc.dto.board.BoardListItemResponse;
import com.oinkvalley.board_svc.dto.board.BoardPostsBundleResponse;
import com.oinkvalley.board_svc.dto.board.BoardResponse;
import com.oinkvalley.board_svc.dto.board.BoardUpdateRequest;
import com.oinkvalley.board_svc.db.repository.BoardRepository;
import com.oinkvalley.board_svc.db.repository.BoardRolePermissionRepository;
import com.oinkvalley.board_svc.security.BoardActor;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

/** 게시판 엔티티 CRUD 및 읽기용 세그먼트→엔티티 해석. 접근 판정은 {@link BoardPermissionService}. */
@Service
@RequiredArgsConstructor
@Transactional
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardRolePermissionRepository permissionRepository;
    private final BoardQueryService boardQueryService;
    private final BoardPermissionService boardPermissionService;

    public BoardResponse create(BoardCreateRequest request) {
        Board board = Board.builder()
                .name(request.name())
                .slug(request.slug())
                .summary(request.summary())
                .postReadPolicy(parsePolicy(request.postReadPolicy()))
                .isActive(request.isActive())
                .build();
        Board saved = boardRepository.save(board);
        seedDefaultPermissions(saved.getId());
        return toResponse(saved);
    }

    /** 신규 보드 기본 권한: USER 전부 허용, 나머지 역할 전부 차단. 이후 조정은 row update. */
    private void seedDefaultPermissions(Long boardId) {
        permissionRepository.save(permission(boardId, "USER", true, true, true));
        permissionRepository.save(permission(boardId, "TEMP_USER", false, false, false));
        permissionRepository.save(permission(boardId, "GUEST", false, false, false));
        permissionRepository.save(permission(boardId, BoardActor.ROLE_ANONYMOUS, false, false, false));
    }

    private static BoardRolePermission permission(
            Long boardId, String role, boolean read, boolean write, boolean comment) {
        return BoardRolePermission.builder()
                .boardId(boardId)
                .role(role)
                .canRead(read)
                .canWrite(write)
                .canComment(comment)
                .build();
    }

    public BoardResponse update(Long boardId, BoardUpdateRequest request) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("Board not found: " + boardId));
        board.update(
                request.name(),
                request.slug(),
                request.summary(),
                parsePolicy(request.postReadPolicy()),
                request.isActive()
        );
        return toResponse(board);
    }

    private static BoardPostReadPolicy parsePolicy(String raw) {
        if (raw == null || raw.isBlank()) {
            return BoardPostReadPolicy.ROLE_READERS;
        }
        try {
            return BoardPostReadPolicy.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "invalid postReadPolicy: " + raw);
        }
    }

    public BoardResponse get(Long boardId) {
        return boardRepository.findById(boardId)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Board not found: " + boardId));
    }

    /** 비활성 보드는 공개 조회 API에서 존재하지 않는 것과 동일하게 404. */
    private void ensureActiveForPublicRead(Board board) {
        if (!board.isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Board not found");
        }
    }

    public Board resolveBoardForPublicRead(String segment) {
        Board board = resolveBoardUnchecked(segment);
        ensureActiveForPublicRead(board);
        return board;
    }

    private Board resolveBoardUnchecked(String segment) {
        if (segment == null || segment.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "board segment must not be blank");
        }
        String s = segment.trim();
        if (s.chars().allMatch(Character::isDigit)) {
            return boardRepository.findById(Long.parseLong(s))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Board not found: " + s));
        }
        return boardRepository.findBySlug(s)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Board not found for slug: " + s));
    }

    public BoardResponse getBoardMetaForRead(String segment) {
        Board board = resolveBoardForPublicRead(segment);
        BoardActor actor = BoardActor.current();
        boardPermissionService.requireRead(actor, board);
        return toViewResponse(board, actor);
    }

    /** 글쓰기 화면 진입: write 권한 필요. */
    public BoardResponse getBoardMetaForWrite(String segment) {
        Board board = resolveBoardForPublicRead(segment);
        BoardActor actor = BoardActor.current();
        boardPermissionService.requireWrite(actor, board);
        return toViewResponse(board, actor);
    }

    public BoardPostsBundleResponse getBoardPostsBundle(String segment, Pageable pageable) {
        Board board = resolveBoardForPublicRead(segment);
        BoardActor actor = BoardActor.current();
        boardPermissionService.requireRead(actor, board);
        return new BoardPostsBundleResponse(
                toViewResponse(board, actor),
                boardQueryService.getPostSummaries(board, actor, pageable)
        );
    }

    /** 활성 보드 전체 목록. 읽기 불가 보드는 {@code canRead=false} 로만 표시한다. */
    public List<BoardListItemResponse> getActiveBoards() {
        BoardActor actor = BoardActor.current();
        Set<Long> readable = boardPermissionService.readableBoardIds(actor);
        return boardRepository.findAllByIsActiveTrueOrderByNameAsc().stream()
                .map(board -> toListItem(board, readable.contains(board.getId())))
                .toList();
    }

    private BoardListItemResponse toListItem(Board board, boolean canRead) {
        return new BoardListItemResponse(
                board.getId(),
                board.getName(),
                board.getSlug(),
                board.getSummary(),
                canRead
        );
    }

    private BoardResponse toResponse(Board board) {
        return toViewResponse(board, null);
    }

    /** 조회 API용. {@code actor} 가 null 이면 {@code canWrite} 는 false. */
    private BoardResponse toViewResponse(Board board, BoardActor actor) {
        boolean canWrite = actor != null && boardPermissionService.canWrite(actor, board.getId());
        return BoardResponse.of(
                board.getId(),
                board.getName(),
                board.getSlug(),
                board.getSummary(),
                board.getPostReadPolicy().name(),
                board.isActive(),
                canWrite,
                board.getCreatedAt(),
                board.getUpdatedAt()
        );
    }
}
