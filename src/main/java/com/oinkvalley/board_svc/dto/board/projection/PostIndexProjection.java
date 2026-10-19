package com.oinkvalley.board_svc.dto.board.projection;

import java.time.Instant;

/** 검색 색인 원천 — 본문 없이 글 소유 필드만. */
public interface PostIndexProjection {

    Long getId();

    Long getBoardId();

    Long getUserId();

    String getTitle();

    Instant getCreatedAt();

    Instant getUpdatedAt();

    boolean getAiGenerated();
}
