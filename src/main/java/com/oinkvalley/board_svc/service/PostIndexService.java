package com.oinkvalley.board_svc.service;

import com.oinkvalley.board_svc.db.repository.PostRepository;
import com.oinkvalley.board_svc.dto.board.projection.PostCommentCountProjection;
import com.oinkvalley.board_svc.dto.board.projection.PostIndexProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 검색 색인(search-svc)용 글 조회 — 권한 판단 없이 원본 상태를 그대로 준다(내부 gRPC 전용, ingress 비노출).
 * 노출 필터는 search-svc 가 listable 게시판 집합(KV LISTABLE_BOARDS)으로 건다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostIndexService {

    public static final int MAX_GET_IDS = 100;
    public static final int MAX_LIST_LIMIT = 500;

    private final PostRepository postRepository;

    public record IndexPost(PostIndexProjection post, long commentCount) {
    }

    public record IndexPage(List<IndexPost> posts, long nextAfterId, boolean hasMore) {
    }

    public List<IndexPost> get(List<Long> postIds) {
        if (postIds.size() > MAX_GET_IDS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "too many post ids (max " + MAX_GET_IDS + ")");
        }
        if (postIds.isEmpty()) {
            return List.of();
        }
        return withCommentCounts(postRepository.findIndexByIds(postIds));
    }

    public IndexPage list(long afterId, int limit) {
        if (limit < 1 || limit > MAX_LIST_LIMIT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be 1.." + MAX_LIST_LIMIT);
        }
        // limit+1 로 다음 페이지 존재 여부 판단
        List<PostIndexProjection> rows = postRepository.findIndexAfter(afterId, PageRequest.of(0, limit + 1));
        boolean hasMore = rows.size() > limit;
        List<PostIndexProjection> page = hasMore ? rows.subList(0, limit) : rows;
        long nextAfterId = page.isEmpty() ? afterId : page.get(page.size() - 1).getId();
        return new IndexPage(withCommentCounts(page), nextAfterId, hasMore);
    }

    private List<IndexPost> withCommentCounts(List<PostIndexProjection> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> ids = rows.stream().map(PostIndexProjection::getId).toList();
        // 댓글 없는 글은 집계 결과에 안 나올 수 있으므로 getOrDefault 로 0 처리 (게시판 목록과 같은 집계)
        Map<Long, Long> counts = postRepository.countCommentsByPostIds(ids).stream()
                .collect(Collectors.toMap(
                        PostCommentCountProjection::getPostId,
                        PostCommentCountProjection::getCommentCount,
                        (left, right) -> left));
        return rows.stream()
                .map(row -> new IndexPost(row, counts.getOrDefault(row.getId(), 0L)))
                .toList();
    }
}
