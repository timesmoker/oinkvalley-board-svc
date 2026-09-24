package com.oinkvalley.board_svc.messaging.event;

import com.oinkvalley.board_svc.db.domain.Post;

/** 게시글 생성 도메인 이벤트. 수신자 정책은 notification-svc. */
public record PostCreatedEvent(Post post) {
}
