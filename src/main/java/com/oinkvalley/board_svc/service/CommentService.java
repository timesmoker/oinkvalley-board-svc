package com.oinkvalley.board_svc.service;

import com.oinkvalley.board_svc.db.domain.Comment;
import com.oinkvalley.board_svc.db.domain.Post;
import com.oinkvalley.board_svc.db.repository.CommentRepository;
import com.oinkvalley.board_svc.db.repository.PostRepository;
import com.oinkvalley.board_svc.dto.board.CommentCreateRequest;
import com.oinkvalley.board_svc.dto.board.CommentResponse;
import com.oinkvalley.board_svc.dto.board.CommentThreadPageResponse;
import com.oinkvalley.board_svc.dto.board.CommentUpdateRequest;
import com.oinkvalley.board_svc.dto.board.projection.PostCommentCountProjection;
import com.oinkvalley.board_svc.messaging.event.CommentCreatedEvent;
import com.oinkvalley.board_svc.messaging.publisher.CommentEventPublisher;
import com.oinkvalley.board_svc.security.BoardActor;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** 댓글 CRUD. parent/root 로 reply target 보존. 삭제는 soft delete. 닉네임은 FE 배치. */
@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final ProseMirrorContentValidator contentValidator;
    private final BoardPermissionService boardPermissionService;
    private final CommentEventPublisher commentEventPublisher;

    public CommentResponse create(Long postId, Long userId, CommentCreateRequest request) {
        contentValidator.validate(request.content());
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + postId));
        ensurePostBoardActiveForPublicRead(post);
        BoardActor actor = BoardActor.current();
        boardPermissionService.requireComment(actor, post.getBoard());
        boardPermissionService.requirePostRead(actor, post.getBoard(), post.getUserId());

        Long parentCommentId = request.parentCommentId();
        Long rootCommentId;
        Comment parent = null;

        if (parentCommentId == null) {
            rootCommentId = null;
        } else {
            parent = commentRepository.findById(parentCommentId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Parent comment not found: " + parentCommentId));
            if (!Objects.equals(parent.getPost().getId(), postId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent comment is on another post");
            }
            if (parent.isDeleted()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot reply to a deleted comment");
            }
            rootCommentId = parent.isRoot() ? parent.getId() : parent.getRootCommentId();
        }

        Comment comment = Comment.builder()
                .post(post)
                .userId(userId)
                .parentCommentId(parentCommentId)
                .rootCommentId(rootCommentId)
                .content(request.content())
                .build();
        comment = commentRepository.save(comment);

        if (comment.getRootCommentId() == null) {
            comment.assignRoot(comment.getId());
            comment = commentRepository.save(comment);
        }

        // 루트 댓글이면 root=null. reply면 parent 가 루트이거나 rootCommentId 로 로드.
        Comment root = null;
        if (parent != null) {
            if (parent.isRoot()) {
                root = parent;
            } else if (rootCommentId != null) {
                root = commentRepository.findById(rootCommentId).orElse(null);
            }
        }
        commentEventPublisher.publish(new CommentCreatedEvent(comment, parent, root, post));

        return toResponse(comment, parentUserIdOf(parent));
    }

    public CommentResponse update(Long commentId, Long userId, CommentUpdateRequest request) {
        contentValidator.validate(request.content());
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found: " + commentId));
        if (comment.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found: " + commentId);
        }
        if (!comment.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No permission to update comment: " + commentId);
        }
        comment.update(request.content());
        return toResponse(comment, resolveParentUserIds(List.of(comment)).get(comment.getId()));
    }

    public void delete(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found: " + commentId));
        if (comment.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found: " + commentId);
        }
        if (!comment.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No permission to delete comment: " + commentId);
        }
        comment.softDelete();
    }

    public CommentResponse get(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found: " + commentId));
        ensurePostBoardActiveForPublicRead(comment.getPost());
        requirePostReadable(comment.getPost());
        return toResponse(comment, resolveParentUserIds(List.of(comment)).get(comment.getId()));
    }

    /**
     * 스레드 페이지: 루트만 size(고정 20)로 자르고, 해당 루트들의 답글은 전부 포함.
     * {@code totalRoots}/{@code totalPages} 는 루트 기준, {@code totalComments} 는 답글 포함.
     */
    public CommentThreadPageResponse getByPost(Long postId, Pageable pageable) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found: " + postId));
        ensurePostBoardActiveForPublicRead(post);
        requirePostReadable(post);

        Pageable sorted = PageableSortDefaults.normalize(pageable);
        Pageable rootPageable = PageRequest.of(
                sorted.getPageNumber(),
                PageableSortDefaults.COMMENT_ROOT_PAGE_SIZE,
                sorted.getSort());

        Page<Comment> roots = commentRepository.findByPost_IdAndParentCommentIdIsNull(postId, rootPageable);
        List<Comment> rootList = roots.getContent();
        List<Long> rootIds = rootList.stream().map(Comment::getId).toList();

        List<Comment> replies = rootIds.isEmpty()
                ? List.of()
                : commentRepository.findByPost_IdAndRootCommentIdInAndParentCommentIdIsNotNull(postId, rootIds);

        List<Comment> combined = new ArrayList<>(rootList.size() + replies.size());
        combined.addAll(rootList);
        combined.addAll(replies);

        Map<Long, Long> parentUserIdByCommentId = resolveParentUserIds(combined);
        List<CommentResponse> content = combined.stream()
                .map(c -> toResponse(c, parentUserIdByCommentId.get(c.getId())))
                .toList();

        long totalComments = postRepository.countCommentsByPostIds(List.of(postId)).stream()
                .findFirst()
                .map(PostCommentCountProjection::getCommentCount)
                .map(c -> c != null ? c : 0L)
                .orElse(0L);

        return new CommentThreadPageResponse(
                content,
                roots.getNumber(),
                roots.getSize(),
                roots.getTotalElements(),
                totalComments,
                roots.getTotalPages()
        );
    }

    /** commentId → parent 작성자 userId. parent 없거나 삭제면 맵에 안 넣음(null). */
    private Map<Long, Long> resolveParentUserIds(List<Comment> comments) {
        Set<Long> parentIds = new HashSet<>();
        for (Comment comment : comments) {
            if (comment.getParentCommentId() != null) {
                parentIds.add(comment.getParentCommentId());
            }
        }
        if (parentIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Comment> parentById = new HashMap<>();
        for (Comment parent : commentRepository.findAllById(parentIds)) {
            parentById.put(parent.getId(), parent);
        }

        Map<Long, Long> out = new HashMap<>();
        for (Comment comment : comments) {
            Long parentId = comment.getParentCommentId();
            if (parentId == null) {
                continue;
            }
            Comment parent = parentById.get(parentId);
            Long parentUserId = parentUserIdOf(parent);
            if (parentUserId != null) {
                out.put(comment.getId(), parentUserId);
            }
        }
        return out;
    }

    private static Long parentUserIdOf(Comment parent) {
        if (parent == null || parent.isDeleted()) {
            return null;
        }
        return parent.getUserId();
    }

    private void ensurePostBoardActiveForPublicRead(Post post) {
        if (!post.getBoard().isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found: " + post.getId());
        }
    }

    private void requirePostReadable(Post post) {
        boardPermissionService.requirePostRead(BoardActor.current(), post.getBoard(), post.getUserId());
    }

    private CommentResponse toResponse(Comment comment, Long parentUserId) {
        return new CommentResponse(
                comment.getId(),
                comment.isDeleted() ? null : comment.getUserId(),
                comment.getPost().getId(),
                comment.getParentCommentId(),
                parentUserId,
                comment.getRootCommentId(),
                comment.isDeleted() ? Map.of("type", "doc", "content", List.of()) : comment.getContent(),
                comment.isDeleted(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
