package com.oinkvalley.board_svc.db.repository;

import com.oinkvalley.board_svc.db.domain.BoardOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface BoardOutboxRepository extends JpaRepository<BoardOutbox, Long> {

    /** 미발행 행을 id 순으로 잠그며 가져온다. 다른 replica 가 잡은 행은 건너뛴다(SKIP LOCKED). */
    @Query(value = """
            SELECT * FROM {h-schema}board_outbox
            WHERE published_at IS NULL
            ORDER BY id
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<BoardOutbox> lockPending(@Param("limit") int limit);

    @Modifying
    @Query("DELETE FROM BoardOutbox o WHERE o.publishedAt IS NOT NULL AND o.publishedAt < :before")
    int deletePublishedBefore(@Param("before") Instant before);
}
