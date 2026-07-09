package com.oinkvalley.board_svc.service;

import com.oinkvalley.board_svc.db.domain.Board;
import com.oinkvalley.board_svc.db.domain.BoardPostReadPolicy;
import com.oinkvalley.board_svc.db.domain.BoardRolePermission;
import com.oinkvalley.board_svc.db.repository.BoardRolePermissionRepository;
import com.oinkvalley.board_svc.security.BoardActor;
import com.oinkvalley.board_svc.security.SecurityJsonHandlers;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 게시판×역할 권한의 단일 판정 지점. {@code board_role_permissions} 행이 없으면 거부(폴백 없음).
 * 거부 시 비로그인은 401, 로그인 상태는 403.
 */
@Service
@RequiredArgsConstructor
public class BoardPermissionService {

    private static final String ROLE_ADMIN = "ADMIN";

    private final BoardRolePermissionRepository permissionRepository;

    public boolean canRead(BoardActor actor, Long boardId) {
        return permissions(actor, boardId).stream().anyMatch(BoardRolePermission::isCanRead);
    }

    public boolean canWrite(BoardActor actor, Long boardId) {
        return permissions(actor, boardId).stream().anyMatch(BoardRolePermission::isCanWrite);
    }

    public boolean canComment(BoardActor actor, Long boardId) {
        return permissions(actor, boardId).stream().anyMatch(BoardRolePermission::isCanComment);
    }

    public void requireRead(BoardActor actor, Board board) {
        if (!canRead(actor, board.getId())) {
            throw denied(actor);
        }
    }

    public void requireWrite(BoardActor actor, Board board) {
        if (!canWrite(actor, board.getId())) {
            throw denied(actor);
        }
    }

    public void requireComment(BoardActor actor, Board board) {
        if (!canComment(actor, board.getId())) {
            throw denied(actor);
        }
    }

    /** 게시판 read 권한 + 글 읽기 정책(post_read_policy)까지 통과해야 글 상세를 읽는다. */
    public boolean canReadPost(BoardActor actor, Board board, Long postAuthorId) {
        if (!canRead(actor, board.getId())) {
            return false;
        }
        return switch (board.getPostReadPolicy()) {
            case ROLE_READERS -> true;
            case AUTHOR_AND_USER -> actor.isAuthorOf(postAuthorId) || actor.hasRole(ROLE_ADMIN);
            case AUTHOR_ONLY -> actor.isAuthorOf(postAuthorId);
        };
    }

    public void requirePostRead(BoardActor actor, Board board, Long postAuthorId) {
        if (!canReadPost(actor, board, postAuthorId)) {
            throw denied(actor);
        }
    }

    /** 목록 마스킹 여부: 정책상 본인 글이 아니면 제목을 감춰야 하는 상태인지. */
    public boolean masksOtherAuthors(BoardActor actor, Board board) {
        return switch (board.getPostReadPolicy()) {
            case ROLE_READERS -> false;
            case AUTHOR_AND_USER -> !actor.hasRole(ROLE_ADMIN);
            case AUTHOR_ONLY -> true;
        };
    }

    public static String maskedTitle(BoardPostReadPolicy policy) {
        return policy == BoardPostReadPolicy.AUTHOR_ONLY
                ? "작성자만 볼 수 있는 게시글입니다."
                : "작성자와 관리자만 볼 수 있는 게시글입니다.";
    }

    /** actor 의 역할로 읽을 수 있는 게시판 ID 집합(목록 필터용). */
    public Set<Long> readableBoardIds(BoardActor actor) {
        return permissionRepository.findByRoleInAndCanReadTrue(actor.roles()).stream()
                .map(BoardRolePermission::getBoardId)
                .collect(Collectors.toUnmodifiableSet());
    }

    private List<BoardRolePermission> permissions(BoardActor actor, Long boardId) {
        return permissionRepository.findByBoardIdAndRoleIn(boardId, actor.roles());
    }

    private ResponseStatusException denied(BoardActor actor) {
        if (!actor.isAuthenticated()) {
            return new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, SecurityJsonHandlers.MESSAGE_LOGIN_REQUIRED);
        }
        return new ResponseStatusException(
                HttpStatus.FORBIDDEN, SecurityJsonHandlers.MESSAGE_MEMBER_REQUIRED);
    }
}
