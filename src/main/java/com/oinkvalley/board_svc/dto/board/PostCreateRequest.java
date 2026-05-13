package com.oinkvalley.board_svc.dto.board;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * 게시글 생성 요청. 작성자 식별자는 요청 본문의 {@code userId}가 아니라 인증(토큰 등)에서 정해집니다.
 */
public record PostCreateRequest(
        @NotNull Long boardId,
        @NotBlank @Size(max = 100) String title,
        @NotNull Map<String, Object> content
) {
}
