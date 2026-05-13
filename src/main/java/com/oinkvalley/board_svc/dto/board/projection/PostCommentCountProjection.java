package com.oinkvalley.board_svc.dto.board.projection;

/**
 * 게시글별 댓글 수 집계 (목록에 붙일 때).
 */
public interface PostCommentCountProjection {

    Long getPostId();

    Long getCommentCount();
}
