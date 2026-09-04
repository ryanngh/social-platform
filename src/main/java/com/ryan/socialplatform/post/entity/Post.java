package com.ryan.socialplatform.post.entity;

import com.ryan.socialplatform.post.enums.PostVisibility;
import com.ryan.socialplatform.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "author_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_posts_author")
    )
    private User author;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    private PostVisibility visibility = PostVisibility.PUBLIC;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public Post(User author, String content, PostVisibility visibility) {
        this.author = Objects.requireNonNull(author, "Author must not be null");
        this.content = content;
        this.visibility = visibility != null ? visibility : PostVisibility.PUBLIC;
    }

    public Post(User author, String content) {
        this(author, content, PostVisibility.PUBLIC);
    }

    @PrePersist
    protected void onCreate() {
        if (this.visibility == null) {
            this.visibility = PostVisibility.PUBLIC;
        }
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public void markDeleted() {
        this.deletedAt = Instant.now();
    }

    public void restore() {
        this.deletedAt = null;
    }
}
