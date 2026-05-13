package com.oinkvalley.board_svc.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * 페이지 목록 정렬 기본값(클라이언트가 {@code sort} 를 주지 않을 때만 적용).
 */
public final class PageableSortDefaults {

    private PageableSortDefaults() {
    }

    /** 생성 시각 역순(최신 먼저). */
    public static Pageable createdAtDescIfUnsorted(Pageable pageable) {
        if (!pageable.getSort().isUnsorted()) {
            return pageable;
        }
        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
