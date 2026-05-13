package com.oinkvalley.board_svc.dto.board;

import java.time.Instant;

/**
 * 게시글 목록 요약 응답 (댓글 수 포함).
 */
public record PostSummaryResponse(
        Long id,
        String title,
        Long userId,
        Instant createdAt,
        long commentCount
) {
}
