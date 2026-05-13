package com.oinkvalley.board_svc.db.repository;

import com.oinkvalley.board_svc.db.domain.Comment;
import com.oinkvalley.board_svc.dto.board.CommentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByPost_Id(Long postId, Pageable pageable);

    Optional<Comment> findByIdAndPost_Id(Long id, Long postId);

    @Query(
            value = """
                    SELECT new com.oinkvalley.board_svc.dto.board.CommentResponse(
                        c.id, c.userId, c.post.id, c.content, c.createdAt, c.updatedAt)
                    FROM Comment c
                    WHERE c.post.id = :postId
                    """,
            countQuery = """
                    SELECT count(c)
                    FROM Comment c
                    WHERE c.post.id = :postId
                    """)
    Page<CommentResponse> findCommentDtosByPostId(@Param("postId") Long postId, Pageable pageable);

    @Query("SELECT p.board.id FROM Comment c JOIN c.post p WHERE c.id = :id")
    Optional<Long> findBoardIdByCommentId(@Param("id") Long id);
}
