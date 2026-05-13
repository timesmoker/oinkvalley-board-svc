package com.oinkvalley.board_svc.dto.board;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * 댓글 생성 요청. 게시글은 URL 쿼리 등으로 식별하고, 작성자 식별자는 인증에서 설정합니다.
 */
public record CommentCreateRequest(
        @NotNull Map<String, Object> content
) {
}
