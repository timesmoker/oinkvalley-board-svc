package com.oinkvalley.board_svc.db.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(
        name = "boards",
        uniqueConstraints = @UniqueConstraint(name = "uk_boards_slug", columnNames = "slug")
)
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ColumnDefault("nextval('boards_id_seq')")
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @Size(max = 100)
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @NotNull
    @Size(max = 64)
    @Column(name = "slug", nullable = false, length = 64)
    private String slug;

    @Size(max = 60)
    @Column(name = "summary", length = 500)
    private String summary;

    @NotNull
    @Column(name = "is_private", nullable = false)
    @Builder.Default
    private boolean isPrivate = false;

    @NotNull
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "board", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Post> posts = new LinkedHashSet<>();

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public void update(String name, String slug, String summary, boolean isPrivate, boolean isActive) {
        this.name = name;
        this.slug = slug;
        this.summary = summary;
        this.isPrivate = isPrivate;
        this.isActive = isActive;
    }
}
