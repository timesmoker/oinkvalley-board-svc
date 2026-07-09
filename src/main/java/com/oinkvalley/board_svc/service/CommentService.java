package com.oinkvalley.board_svc.service;

import com.oinkvalley.board_svc.db.domain.Comment;
import com.oinkvalley.board_svc.db.domain.Post;
import com.oinkvalley.board_svc.dto.board.CommentCreateRequest;
import com.oinkvalley.board_svc.dto.board.CommentResponse;
import com.oinkvalley.board_svc.dto.board.CommentUpdateRequest;
import com.oinkvalley.board_svc.db.repository.CommentRepository;
import com.oinkvalley.board_svc.db.repository.PostRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** 댓글 CRUD. 수정·삭제는 요청 사용자 ID 와 작성자 ID 가 일치할 때만 허용합니다. */
@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final ProseMirrorContentValidator contentValidator;

    public CommentResponse create(Long postId, Long userId, CommentCreateRequest request) {
        contentValidator.validate(request.content());
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + postId));
        Comment comment = Comment.builder()
                .post(post)
                .userId(userId)
                .content(request.content())
                .build();
        return toResponse(commentRepository.save(comment));
    }

    @Transactional
    public CommentResponse update(Long commentId, Long userId, CommentUpdateRequest request) {
        contentValidator.validate(request.content());
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found: " + commentId));
        if (!comment.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No permission to update comment: " + commentId);
        }
        comment.update(request.content());
        return toResponse(comment);
    }

    @Transactional
    public void delete(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found: " + commentId));
        if (!comment.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No permission to delete comment: " + commentId);
        }
        commentRepository.delete(comment);
    }

    @Transactional
    public CommentResponse get(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found: " + commentId));
        ensurePostBoardActiveForPublicRead(comment.getPost());
        return toResponse(comment);
    }

    @Transactional
    public Page<CommentResponse> getByPost(Long postId, Pageable pageable) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found: " + postId));
        ensurePostBoardActiveForPublicRead(post);
        Pageable sorted = PageableSortDefaults.createdAtDescIfUnsorted(pageable);
        return commentRepository.findByPost_Id(postId, sorted)
                .map(this::toResponse);
    }

    private void ensurePostBoardActiveForPublicRead(Post post) {
        if (!post.getBoard().isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found: " + post.getId());
        }
    }

    private CommentResponse toResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getUserId(),
                comment.getPost().getId(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
