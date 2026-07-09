package com.oinkvalley.board_svc.controller;

import com.oinkvalley.board_svc.db.domain.Board;
import com.oinkvalley.board_svc.dto.board.BoardCreateRequest;
import com.oinkvalley.board_svc.dto.board.BoardListItemResponse;
import com.oinkvalley.board_svc.dto.board.BoardPostsBundleResponse;
import com.oinkvalley.board_svc.dto.board.BoardResponse;
import com.oinkvalley.board_svc.dto.board.BoardUpdateRequest;
import com.oinkvalley.board_svc.dto.board.PostResponse;
import com.oinkvalley.board_svc.service.BoardService;
import com.oinkvalley.board_svc.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 프런트 라우트와 맞춘 게시판·글 읽기 경로.
 * <p>매핑 순서: {@code /{segment}/write}, {@code /{segment}/{postId}} 가 {@code /{segment}} 보다 먼저 등록되어
 * {@code write} 나 숫자 {@code postId} 가 슬러그로 오인되지 않습니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/boards")
public class BoardController {

    private final BoardService boardService;
    private final PostService postService;

    @PostMapping
    public ResponseEntity<BoardResponse> create(@Valid @RequestBody BoardCreateRequest request) {
        return ResponseEntity.ok(boardService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<BoardListItemResponse>> getActiveBoards() {
        return ResponseEntity.ok(boardService.getActiveBoards());
    }

    /** 글쓰기 화면(서버 컴포넌트용) 게시판 메타만. 실제 글 생성은 {@code POST /posts}. */
    @GetMapping("/{segment}/write")
    public ResponseEntity<BoardResponse> getBoardForWrite(@PathVariable String segment) {
        return ResponseEntity.ok(boardService.getBoardMetaForWrite(segment));
    }

    @GetMapping("/{segment}/{postId:\\d+}")
    public ResponseEntity<PostResponse> getPostInBoard(
            @PathVariable String segment,
            @PathVariable Long postId
    ) {
        Board board = boardService.resolveBoardForPublicRead(segment);
        return ResponseEntity.ok(postService.getInBoard(postId, board));
    }

    /** 게시판 메타와 글 요약 목록(게시판 홈). 세그먼트가 숫자만이면 기본키, 아니면 슬러그로 해석합니다. */
    @GetMapping("/{segment}")
    public ResponseEntity<BoardPostsBundleResponse> getBoardHome(
            @PathVariable String segment,
            Pageable pageable
    ) {
        return ResponseEntity.ok(boardService.getBoardPostsBundle(segment, pageable));
    }

    @PutMapping("/{boardId:\\d+}")
    public ResponseEntity<BoardResponse> update(
            @PathVariable Long boardId,
            @Valid @RequestBody BoardUpdateRequest request
    ) {
        return ResponseEntity.ok(boardService.update(boardId, request));
    }
}
