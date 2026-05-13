package com.oinkvalley.board_svc.dto.board;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * 댓글 수정 요청.
 */
public record CommentUpdateRequest(
        @NotNull Map<String, Object> content
) {
}
