package com.oinkvalley.board_svc.controller;

import com.oinkvalley.board_svc.dto.board.CommentCreateRequest;
import com.oinkvalley.board_svc.dto.board.CommentResponse;
import com.oinkvalley.board_svc.dto.board.CommentUpdateRequest;
import com.oinkvalley.board_svc.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** 댓글 CRUD. 목록·단건 조회는 {@code postId} 쿼리 또는 경로로 게시글과 연결됩니다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/comments")
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentResponse> create(
            @RequestParam("postId") Long postId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        return ResponseEntity.ok(commentService.create(postId, userId, request));
    }

    @GetMapping("/{commentId}")
    public ResponseEntity<CommentResponse> get(@PathVariable Long commentId) {
        return ResponseEntity.ok(commentService.get(commentId));
    }

    @GetMapping
    public ResponseEntity<Page<CommentResponse>> getByPost(
            @RequestParam("postId") Long postId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(commentService.getByPost(postId, pageable));
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponse> update(
            @PathVariable Long commentId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CommentUpdateRequest request
    ) {
        return ResponseEntity.ok(commentService.update(commentId, userId, request));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long commentId,
            @AuthenticationPrincipal Long userId
    ) {
        commentService.delete(commentId, userId);
        return ResponseEntity.noContent().build();
    }
}
