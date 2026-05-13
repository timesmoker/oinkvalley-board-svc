package com.oinkvalley.board_svc.db.repository;

import com.oinkvalley.board_svc.db.domain.Post;
import com.oinkvalley.board_svc.dto.board.projection.PostBaseProjection;
import com.oinkvalley.board_svc.dto.board.projection.PostCommentCountProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** 게시글 저장소. 목록용 프로젝션·댓글 수 배치 집계는 게시판 홈 응답 최적화용입니다. */
public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByBoard_Id(Long boardId, Pageable pageable);

    Optional<Post> findByIdAndBoard_Id(Long id, Long boardId);

    @Query("""
            SELECT p.id AS id, p.title AS title, p.userId AS userId, p.createdAt AS createdAt
            FROM Post p
            WHERE p.board.id = :boardId
            """)
    Page<PostBaseProjection> findPostBaseList(@Param("boardId") Long boardId, Pageable pageable);

    @Query("""
            SELECT p.id AS postId, COUNT(c) AS commentCount
            FROM Post p
            LEFT JOIN p.comments c
            WHERE p.id IN :postIds
            GROUP BY p.id
            """)
    List<PostCommentCountProjection> countCommentsByPostIds(@Param("postIds") List<Long> postIds);

    @Query("SELECT p.board.id FROM Post p WHERE p.id = :id")
    Optional<Long> findBoardIdByPostId(@Param("id") Long id);
}
