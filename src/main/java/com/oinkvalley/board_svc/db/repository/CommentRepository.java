package com.oinkvalley.board_svc.db.repository;

import com.oinkvalley.board_svc.db.domain.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /** flat 시간순 페이지 (레거시). 현재 목록은 스레드 페이지 사용. */
    Page<Comment> findByPost_Id(Long postId, Pageable pageable);

    /** 최상위 댓글만 페이지 (스레드 단위). */
    Page<Comment> findByPost_IdAndParentCommentIdIsNull(Long postId, Pageable pageable);

    /** 지정 루트들에 속한 답글(루트 자신 제외). */
    List<Comment> findByPost_IdAndRootCommentIdInAndParentCommentIdIsNotNull(
            Long postId,
            Collection<Long> rootCommentIds
    );

    Optional<Comment> findByIdAndPost_Id(Long id, Long postId);

    @Query("SELECT p.board.id FROM Comment c JOIN c.post p WHERE c.id = :id")
    Optional<Long> findBoardIdByCommentId(@Param("id") Long id);
}
