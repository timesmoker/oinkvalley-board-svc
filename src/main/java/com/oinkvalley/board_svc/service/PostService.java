package com.oinkvalley.board_svc.service;

import com.oinkvalley.board_svc.db.domain.Board;
import com.oinkvalley.board_svc.db.domain.Post;
import com.oinkvalley.board_svc.dto.board.PostCreateRequest;
import com.oinkvalley.board_svc.dto.board.PostResponse;
import com.oinkvalley.board_svc.dto.board.PostUpdateRequest;
import com.oinkvalley.board_svc.dto.internal.InternalPostCreateRequest;
import com.oinkvalley.board_svc.dto.internal.InternalPostCreateResponse;
import com.oinkvalley.board_svc.db.repository.BoardRepository;
import com.oinkvalley.board_svc.db.repository.PostRepository;
import jakarta.transaction.Transactional;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** 게시글 작성·수정·삭제 및 게시판 경로 하위 단건 조회. */
@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

    private final PostRepository postRepository;
    private final BoardRepository boardRepository;

    public PostResponse create(Long userId, PostCreateRequest request) {
        Board board = boardRepository.findById(request.boardId())
                .orElseThrow(() -> new IllegalArgumentException("Board not found: " + request.boardId()));
        Post post = Post.builder()
                .userId(userId)
                .board(board)
                .title(request.title())
                .content(request.content())
                .build();
        return toResponse(postRepository.save(post));
    }

    /** {@code POST /internal/posts} — 작성자 ID 는 요청 본문의 {@code authorUserId}. */
    public InternalPostCreateResponse createInternal(InternalPostCreateRequest request) {
        Board board = boardRepository.findById(request.boardId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Board not found: " + request.boardId()));
        if (!board.isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Board not found");
        }
        Post post = Post.builder()
                .userId(request.authorUserId())
                .board(board)
                .title(request.title())
                .content(PostContentFactory.fromPlainText(
                        request.text(), request.sourceUrl(), bubblePalMetadata(request)))
                .build();
        return new InternalPostCreateResponse(postRepository.save(post).getId());
    }

    private static Map<String, Object> bubblePalMetadata(InternalPostCreateRequest request) {
        if (request.metadata() == null || request.metadata().isEmpty()) {
            return null;
        }
        Map<String, Object> meta = new LinkedHashMap<>(request.metadata());
        if (request.characterId() != null && !request.characterId().isBlank()) {
            meta.putIfAbsent("characterId", request.characterId().trim());
        }
        if (request.authorType() != null && !request.authorType().isBlank()) {
            meta.putIfAbsent("authorType", request.authorType().trim());
        }
        return meta;
    }

    @Transactional
    public PostResponse update(Long postId, Long userId, PostUpdateRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found: " + postId));
        if (!post.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No permission to update post: " + postId);
        }
        post.update(request.title(), request.content());
        return toResponse(post);
    }

    @Transactional
    public void delete(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found: " + postId));
        if (!post.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No permission to delete post: " + postId);
        }
        postRepository.delete(post);
    }

    /**
     * {@code GET /boards/{segment}/{postId}} — 글이 해당 게시판에 속할 때만 200.
     */
    @Transactional
    public PostResponse getInBoard(Long postId, Long boardId) {
        Post post = postRepository.findByIdAndBoard_Id(postId, boardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found in board: " + postId));
        return toResponse(post);
    }

    private PostResponse toResponse(Post post) {
        return new PostResponse(
                post.getId(),
                post.getUserId(),
                post.getBoard().getId(),
                post.getTitle(),
                post.getContent(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
