package com.ryan.socialplatform.comment.entity;

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
@Table(name = "comment_mentions")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentMention {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "comment_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_comment_mentions_comment")
    )
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "mentioned_user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_comment_mentions_mentioned_user")
    )
    private User mentionedUser;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public CommentMention(Comment comment, User mentionedUser) {
        this.comment = Objects.requireNonNull(comment, "Comment must not be null");
        this.mentionedUser = Objects.requireNonNull(mentionedUser, "Mentioned user must not be null");
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
