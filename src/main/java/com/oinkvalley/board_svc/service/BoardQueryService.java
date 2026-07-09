package com.oinkvalley.board_svc.service;

import com.oinkvalley.board_svc.db.domain.Board;
import com.oinkvalley.board_svc.dto.board.PostSummaryResponse;
import com.oinkvalley.board_svc.dto.board.projection.PostBaseProjection;
import com.oinkvalley.board_svc.dto.board.projection.PostCommentCountProjection;
import com.oinkvalley.board_svc.db.repository.PostRepository;
import com.oinkvalley.board_svc.security.BoardActor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 게시판 글 목록용 집계. 본문 JSON을 안 읽는 경량 쿼리로 글 목록을 가져온 뒤,
 * 한 번 더 쿼리해 댓글 수만 맵으로 붙입니다.
 * post_read_policy 에 따라 본인 글이 아닌 제목은 마스킹합니다.
 */
@Service
@RequiredArgsConstructor
public class BoardQueryService {

    private final PostRepository postRepository;
    private final BoardPermissionService boardPermissionService;

    public Page<PostSummaryResponse> getPostSummaries(Board board, BoardActor actor, Pageable pageable) {
        Pageable sorted = PageableSortDefaults.createdAtDescIfUnsorted(pageable);
        Page<PostBaseProjection> basePage = postRepository.findPostBaseList(board.getId(), sorted);

        List<Long> postIds = basePage.getContent().stream()
                .map(PostBaseProjection::getId)
                .toList();

        if (postIds.isEmpty()) {
            return new PageImpl<>(List.of(), sorted, basePage.getTotalElements());
        }

        // 댓글 없는 글은 집계 결과에 안 나올 수 있으므로 getOrDefault 로 0 처리
        Map<Long, Long> commentCountMap = postRepository.countCommentsByPostIds(postIds).stream()
                .collect(Collectors.toMap(
                        PostCommentCountProjection::getPostId,
                        PostCommentCountProjection::getCommentCount,
                        (left, right) -> left
                ));

        boolean maskOthers = boardPermissionService.masksOtherAuthors(actor, board);
        String maskedTitle = BoardPermissionService.maskedTitle(board.getPostReadPolicy());

        List<PostSummaryResponse> content = basePage.getContent().stream()
                .map(base -> new PostSummaryResponse(
                        base.getId(),
                        maskOthers && !actor.isAuthorOf(base.getUserId()) ? maskedTitle : base.getTitle(),
                        base.getUserId(),
                        base.getCreatedAt(),
                        commentCountMap.getOrDefault(base.getId(), 0L)
                ))
                .toList();

        return new PageImpl<>(content, sorted, basePage.getTotalElements());
    }
}
