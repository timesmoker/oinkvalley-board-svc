package com.oinkvalley.board_svc.controller;

import com.oinkvalley.board_svc.dto.board.PostCreateRequest;
import com.oinkvalley.board_svc.dto.board.PostResponse;
import com.oinkvalley.board_svc.dto.board.PostUpdateRequest;
import com.oinkvalley.board_svc.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 게시글 생성·수정·삭제. 게시글 **조회**는 {@code GET /boards/{segment}/{postId}} 만 제공합니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;

    @PostMapping
    public ResponseEntity<PostResponse> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PostCreateRequest request
    ) {
        return ResponseEntity.ok(postService.create(userId, request));
    }

    @PutMapping("/{postId}")
    public ResponseEntity<PostResponse> update(
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PostUpdateRequest request
    ) {
        return ResponseEntity.ok(postService.update(postId, userId, request));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId
    ) {
        postService.delete(postId, userId);
        return ResponseEntity.noContent().build();
    }
}
