package com.oinkvalley.board_svc.dto.board;

import java.time.Instant;
import java.util.Map;

/**
 * 댓글 응답.
 */
public record CommentResponse(
        Long id,
        Long userId,
        Long postId,
        Map<String, Object> content,
        Instant createdAt,
        Instant updatedAt
) {
}
