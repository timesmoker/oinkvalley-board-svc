package com.oinkvalley.board_svc.dto.board;

/**
 * 게시판 목록 항목. {@code GET /boards} 전용.
 * {@code canRead} 는 현재 요청 주체 기준 읽기 가능 여부 — UI 에서 비공개 표시·진입 차단에만 쓴다.
 */
public record BoardListItemResponse(
        Long id,
        String name,
        String slug,
        String summary,
        boolean canRead
) {
}
