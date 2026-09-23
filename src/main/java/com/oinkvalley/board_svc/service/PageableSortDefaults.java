package com.oinkvalley.board_svc.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 목록 {@link Pageable} 정규화.
 * <ul>
 *   <li>sort 없으면 {@code createdAt DESC}</li>
 *   <li>허용 필드: {@code createdAt} 만</li>
 *   <li>size 1~{@link #MAX_PAGE_SIZE}</li>
 * </ul>
 */
public final class PageableSortDefaults {

    public static final int MAX_PAGE_SIZE = 50;
    public static final int DEFAULT_PAGE_SIZE = 10;
    /** 댓글 스레드 페이지 — 루트 개수 고정. */
    public static final int COMMENT_ROOT_PAGE_SIZE = 20;

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of("createdAt");

    private PageableSortDefaults() {
    }

    public static Pageable normalize(Pageable pageable) {
        int size = pageable.getPageSize();
        if (size < 1) {
            size = DEFAULT_PAGE_SIZE;
        } else if (size > MAX_PAGE_SIZE) {
            size = MAX_PAGE_SIZE;
        }

        Sort sort = sanitizeSort(pageable.getSort());
        return PageRequest.of(pageable.getPageNumber(), size, sort);
    }

    private static Sort sanitizeSort(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        List<Sort.Order> allowed = new ArrayList<>();
        for (Sort.Order order : sort) {
            if (ALLOWED_SORT_PROPERTIES.contains(order.getProperty())) {
                allowed.add(order);
            }
        }
        if (allowed.isEmpty()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        return Sort.by(allowed);
    }
}
