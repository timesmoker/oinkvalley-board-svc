package com.oinkvalley.board_svc.dto.board;

import java.time.Instant;
import java.util.Map;

/**
 * 게시글 응답.
 */
public record PostResponse(
        Long id,
        Long userId,
        Long boardId,
        String title,
        Map<String, Object> content,
        Instant createdAt,
        Instant updatedAt
) {
}
