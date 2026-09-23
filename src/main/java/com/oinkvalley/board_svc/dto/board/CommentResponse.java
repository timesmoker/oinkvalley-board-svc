package com.oinkvalley.board_svc.dto.board;

import java.time.Instant;
import java.util.Map;

/**
 * 댓글 응답. UI depth 는 root 여부만. parent 는 reply target 식별용.
 * {@code parentUserId} 는 board 가 parent 댓글에서 조회해 채움 (닉은 FE 배치).
 */
public record CommentResponse(
        Long id,
        Long userId,
        Long postId,
        Long parentCommentId,
        Long parentUserId,
        Long rootCommentId,
        Map<String, Object> content,
        boolean deleted,
        Instant createdAt,
        Instant updatedAt
) {
}
