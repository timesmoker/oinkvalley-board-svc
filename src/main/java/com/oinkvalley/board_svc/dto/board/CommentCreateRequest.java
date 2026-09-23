package com.oinkvalley.board_svc.dto.board;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * 댓글 생성. {@code parentCommentId} 있으면 해당 댓글에 대한 답글.
 * {@code rootCommentId} 는 서버가 parent 기준으로 계산한다.
 */
public record CommentCreateRequest(
        @NotNull Map<String, Object> content,
        Long parentCommentId
) {
}
