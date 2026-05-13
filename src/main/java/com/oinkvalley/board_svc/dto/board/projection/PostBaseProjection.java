package com.oinkvalley.board_svc.dto.board.projection;

import java.time.Instant;

/**
 * 게시글 목록용 — 본문·연관 그래프 없이 최소 컬럼만.
 */
public interface PostBaseProjection {

    Long getId();

    String getTitle();

    Long getUserId();

    Instant getCreatedAt();
}
