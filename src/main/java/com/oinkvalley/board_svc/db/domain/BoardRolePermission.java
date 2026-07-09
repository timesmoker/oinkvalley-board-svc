package com.oinkvalley.board_svc.db.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/** 게시판×역할 권한 행. 읽기/쓰기/댓글 가능 여부의 단일 기준. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@IdClass(BoardRolePermission.Pk.class)
@Table(name = "board_role_permissions")
public class BoardRolePermission {

    @Id
    @Column(name = "board_id", nullable = false)
    private Long boardId;

    @Id
    @Column(name = "role", nullable = false, length = 64)
    private String role;

    @Column(name = "can_read", nullable = false)
    private boolean canRead;

    @Column(name = "can_write", nullable = false)
    private boolean canWrite;

    @Column(name = "can_comment", nullable = false)
    private boolean canComment;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pk implements Serializable {
        private Long boardId;
        private String role;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pk pk)) return false;
            return Objects.equals(boardId, pk.boardId) && Objects.equals(role, pk.role);
        }

        @Override
        public int hashCode() {
            return Objects.hash(boardId, role);
        }
    }
}
