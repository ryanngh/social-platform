package com.ryan.socialplatform.comment.entity;

import com.ryan.socialplatform.post.entity.Post;
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
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "post_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_comments_post")
    )
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "author_id",
            foreignKey = @ForeignKey(name = "fk_comments_author")
    )
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "parent_comment_id",
            foreignKey = @ForeignKey(name = "fk_comments_parent_comment")
    )
    private Comment parentComment;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "like_count", nullable = false)
    private int likeCount = 0;

    @Column(name = "reply_count", nullable = false)
    private int replyCount = 0;

    @Column(name = "is_pinned", nullable = false)
    private boolean pinned = false;

    @Column(name = "pinned_at")
    private Instant pinnedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "edited_at")
    private Instant editedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public Comment(Post post, User author, String content) {
        this.post = Objects.requireNonNull(post, "Post must not be null");
        this.author = author;
        this.content = content;
    }

    public Comment(Post post, User author, Comment parentComment, String content) {
        this.post = Objects.requireNonNull(post, "Post must not be null");
        this.author = author;
        this.parentComment = parentComment;
        this.content = content;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
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

    public boolean isEdited() {
        return this.editedAt != null;
    }

    public void markEdited() {
        this.editedAt = Instant.now();
    }

    public void updateContent(String newContent) {
        this.content = newContent;
        this.editedAt = Instant.now();
    }

    public boolean isTopLevel() {
        return this.parentComment == null;
    }

    public void pin() {
        if (!isTopLevel()) {
            throw new IllegalStateException("Only top-level comments can be pinned");
        }
        this.pinned = true;
        this.pinnedAt = Instant.now();
    }

    public void unpin() {
        this.pinned = false;
        this.pinnedAt = null;
    }
}
