package com.oinkvalley.board_svc.messaging.event;

import com.oinkvalley.board_svc.db.domain.Comment;
import com.oinkvalley.board_svc.db.domain.Post;

/**
 * 댓글 생성 도메인 이벤트.
 * parent = 직계 부모 (루트면 null). root = 스레드 루트 (루트 댓글이면 null 또는 self 미포함).
 */
public record CommentCreatedEvent(
        Comment comment,
        Comment parent,
        Comment root,
        Post post
) {
}
