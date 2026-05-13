package com.oinkvalley.board_svc.service;

import com.oinkvalley.board_svc.db.domain.Board;
import com.oinkvalley.board_svc.dto.board.BoardCreateRequest;
import com.oinkvalley.board_svc.dto.board.BoardPostsBundleResponse;
import com.oinkvalley.board_svc.dto.board.BoardResponse;
import com.oinkvalley.board_svc.dto.board.BoardUpdateRequest;
import com.oinkvalley.board_svc.db.repository.BoardRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** 게시판 엔티티 CRUD 및 공개 읽기용 세그먼트→엔티티 해석. */
@Service
@RequiredArgsConstructor
@Transactional
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardQueryService boardQueryService;

    public BoardResponse create(BoardCreateRequest request) {
        Board board = Board.builder()
                .name(request.name())
                .slug(request.slug())
                .summary(request.summary())
                .isPrivate(request.isPrivate())
                .isActive(request.isActive())
                .build();
        return toResponse(boardRepository.save(board));
    }

    public BoardResponse update(Long boardId, BoardUpdateRequest request) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("Board not found: " + boardId));
        board.update(
                request.name(),
                request.slug(),
                request.summary(),
                request.isPrivate(),
                request.isActive()
        );
        return toResponse(board);
    }

    public BoardResponse get(Long boardId) {
        return boardRepository.findById(boardId)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Board not found: " + boardId));
    }

    /**
     * URL 첫 세그먼트: 숫자만이면 기본키, 아니면 슬러그로 해석합니다.
     */
    public Board resolveBoardForPublicRead(String segment) {
        if (segment == null || segment.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "board segment must not be blank");
        }
        String s = segment.trim();
        if (s.chars().allMatch(Character::isDigit)) {
            return boardRepository.findById(Long.parseLong(s))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Board not found: " + s));
        }
        return boardRepository.findBySlug(s)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Board not found for slug: " + s));
    }

    public BoardResponse getBoardMetaForRead(String segment) {
        return toResponse(resolveBoardForPublicRead(segment));
    }

    public BoardPostsBundleResponse getBoardPostsBundle(String segment, Pageable pageable) {
        Board board = resolveBoardForPublicRead(segment);
        return new BoardPostsBundleResponse(
                toResponse(board),
                boardQueryService.getPostSummaries(board.getId(), pageable)
        );
    }

    public List<BoardResponse> getActiveBoards() {
        return boardRepository.findAllByIsActiveTrueOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    private BoardResponse toResponse(Board board) {
        return new BoardResponse(
                board.getId(),
                board.getName(),
                board.getSlug(),
                board.getSummary(),
                board.isPrivate(),
                board.isActive(),
                board.getCreatedAt(),
                board.getUpdatedAt()
        );
    }
}
