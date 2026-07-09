package com.oinkvalley.board_svc.dto.board;

import java.time.Instant;

/**
 * 게시판 단건 메타데이터. {@code GET /boards/{segment}} 번들의 {@code board} 필드, {@code GET /boards/{segment}/write},
 * {@code GET /boards} 목록 항목, 게시판 생성·수정 응답 등에 사용합니다.
 */
public record BoardResponse(
        Long id,
        String name,
        String slug,
        String summary,
        String postReadPolicy,
        boolean isActive,
        boolean canWrite,
        Instant createdAt,
        Instant updatedAt
) {

    public static BoardResponse of(
            Long id,
            String name,
            String slug,
            String summary,
            String postReadPolicy,
            boolean isActive,
            boolean canWrite,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new BoardResponse(
                id, name, slug, summary, postReadPolicy, isActive, canWrite, createdAt, updatedAt);
    }
}
