package com.oinkvalley.board_svc.dto.board;

import org.springframework.data.domain.Page;

/**
 * {@code GET /boards/{segment}} — 게시판 메타데이터와 글 요약 목록을 한 번에 반환합니다.
 */
public record BoardPostsBundleResponse(
        BoardResponse board,
        Page<PostSummaryResponse> posts
) {
}
