package com.oinkvalley.board_svc.controller;

import com.oinkvalley.board_svc.dto.internal.InternalPostCreateRequest;
import com.oinkvalley.board_svc.dto.internal.InternalPostCreateResponse;
import com.oinkvalley.board_svc.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** bubble-pal 등 클러스터 내부 서비스 전용. ingress 에 노출하지 않는다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/posts")
public class InternalPostController {

    private final PostService postService;

    @PostMapping
    public ResponseEntity<InternalPostCreateResponse> create(
            @Valid @RequestBody InternalPostCreateRequest request
    ) {
        return ResponseEntity.ok(postService.createInternal(request));
    }
}
