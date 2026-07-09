package com.oinkvalley.board_svc.dto.board;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 게시판 생성 요청.
 */
public record BoardCreateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 64) String slug,
        @Size(max = 500) String summary,
        @Size(max = 32) String postReadPolicy,
        @NotNull Boolean isActive
) {
}
