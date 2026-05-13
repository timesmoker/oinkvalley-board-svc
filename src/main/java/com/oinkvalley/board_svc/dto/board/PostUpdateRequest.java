package com.oinkvalley.board_svc.dto.board;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * 게시글 수정 요청.
 */
public record PostUpdateRequest(
        @NotBlank @Size(max = 100) String title,
        @NotNull Map<String, Object> content
) {
}
