package com.oinkvalley.board_svc.db.domain;

/** 게시판 글 상세 읽기 정책. read 권한과 별개로 글 단위 노출 범위를 정한다. */
public enum BoardPostReadPolicy {
    /** 게시판 read 권한이 있으면 모든 글을 읽는다. */
    ROLE_READERS,
    /** 작성자 본인 + ADMIN 역할만 글을 읽는다. */
    AUTHOR_AND_USER,
    /** 작성자 본인만 글을 읽는다. */
    AUTHOR_ONLY
}
