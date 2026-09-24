package com.oinkvalley.board_svc.messaging.publisher;

import com.oinkvalley.board_svc.config.NatsConfig;
import com.oinkvalley.board_svc.db.domain.Post;
import com.oinkvalley.board_svc.messaging.event.PostCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 게시글 생성 도메인 사실 → {@code board.post.created}.
 * 팔로워 알림 등은 notification-svc 정책.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostEventPublisher {

    public static final String SUBJECT = "board.post.created";

    private final NatsConfig natsConfig;

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

        try {
            natsConfig.publish(SUBJECT, body);
        } catch (Exception e) {
            log.warn("board.post.created publish failed postId={}: {}", post.getId(), e.toString());
        }
    }
}
