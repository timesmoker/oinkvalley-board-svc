package com.oinkvalley.board_svc.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.oinkvalley.board_svc.db.domain.Board;

import java.util.List;
import java.util.Optional;

public interface BoardRepository extends JpaRepository<Board, Long> {

    Optional<Board> findBySlug(String slug);

    List<Board> findAllByIsActiveTrueOrderByNameAsc();
}
