package com.oinkvalley.board_svc.dto.board;

import java.util.List;

/**
 * 댓글 스레드 페이지.
 * {@code totalRoots}/{@code totalPages} 는 루트 기준 페이징,
 * {@code totalComments} 는 답글 포함 전체 개수(표시용).
 */
public record CommentThreadPageResponse(
        List<CommentResponse> content,
        int number,
        int size,
        long totalRoots,
        long totalComments,
        int totalPages
) {
}
