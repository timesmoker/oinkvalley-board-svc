package com.oinkvalley.board_svc.messaging.publisher;

import com.oinkvalley.board_svc.db.domain.Comment;
import com.oinkvalley.board_svc.db.domain.Post;
import com.oinkvalley.board_svc.messaging.event.CommentCreatedEvent;
import com.oinkvalley.board_svc.messaging.outbox.OutboxWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 댓글 생성 도메인 사실 → {@code board.comment.created}.
 * 수신자·문구·dedupe 는 notification-svc 가 결정. outbox 기록 (발행은 OutboxRelay).
 */
@Component
@RequiredArgsConstructor
public class CommentEventPublisher {

    public static final String SUBJECT = "board.comment.created";

    private final OutboxWriter outboxWriter;

    public void publish(CommentCreatedEvent event) {
        if (event == null || event.comment() == null || event.post() == null) {
            return;
        }
        Comment comment = event.comment();
        Comment parent = event.parent();
        Comment root = event.root();
        Post post = event.post();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("commentId", comment.getId());
        body.put("postId", post.getId());
        body.put("boardId", post.getBoard() != null ? post.getBoard().getId() : null);
        body.put("boardSlug", post.getBoard() != null ? post.getBoard().getSlug() : null);
        body.put("actorUserId", comment.getUserId());
        body.put("postAuthorUserId", post.getUserId());
        body.put("parentCommentId", parent != null ? parent.getId() : null);
        body.put("parentAuthorUserId", parent != null ? parent.getUserId() : null);
        body.put("rootCommentId", root != null ? root.getId() : null);
        body.put("rootAuthorUserId", root != null ? root.getUserId() : null);

        outboxWriter.record(SUBJECT, body);
    }
}
