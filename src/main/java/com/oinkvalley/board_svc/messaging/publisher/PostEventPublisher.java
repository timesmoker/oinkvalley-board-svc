package com.oinkvalley.board_svc.messaging.publisher;

import com.oinkvalley.board_svc.db.domain.Post;
import com.oinkvalley.board_svc.messaging.event.PostCreatedEvent;
import com.oinkvalley.board_svc.messaging.outbox.OutboxWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 게시글 도메인 사실 → outbox 기록 (발행은 OutboxRelay). 글 작성·수정·삭제 트랜잭션 안에서 호출.
 * 수정·삭제 payload 는 postId·boardId 만 — 수신자(search-svc)는 현재 상태를 다시 읽는다.
 */
@Component
@RequiredArgsConstructor
public class PostEventPublisher {

    public static final String SUBJECT = "board.post.created";
    public static final String SUBJECT_UPDATED = "board.post.updated";
    public static final String SUBJECT_DELETED = "board.post.deleted";

    private final OutboxWriter outboxWriter;

    public void publish(PostCreatedEvent event) {
        if (event == null || event.post() == null) {
            return;
        }
        Post post = event.post();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("postId", post.getId());
        body.put("boardId", post.getBoard() != null ? post.getBoard().getId() : null);
        body.put("boardSlug", post.getBoard() != null ? post.getBoard().getSlug() : null);
        body.put("authorUserId", post.getUserId());
        body.put("title", post.getTitle() == null ? "" : post.getTitle());

        outboxWriter.record(SUBJECT, body);
    }

    public void publishUpdated(Post post) {
        outboxWriter.record(SUBJECT_UPDATED, idBody(post));
    }

    public void publishDeleted(Post post) {
        outboxWriter.record(SUBJECT_DELETED, idBody(post));
    }

    private static Map<String, Object> idBody(Post post) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("postId", post.getId());
        body.put("boardId", post.getBoard() != null ? post.getBoard().getId() : null);
        return body;
    }
}
