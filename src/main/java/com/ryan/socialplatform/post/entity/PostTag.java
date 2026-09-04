package com.ryan.socialplatform.post.entity;

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
@Table(name = "post_tags")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostTag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "post_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_post_tags_post")
    )
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "tagged_user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_post_tags_tagged_user")
    )
    private User taggedUser;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PostTag(Post post, User taggedUser) {
        this.post = Objects.requireNonNull(post, "Post must not be null");
        this.taggedUser = Objects.requireNonNull(taggedUser, "Tagged user must not be null");
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
