package com.ryan.socialplatform.post.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "post_hashtags")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostHashtag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "post_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_post_hashtags_post")
    )
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "hashtag_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_post_hashtags_hashtag")
    )
    private Hashtag hashtag;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PostHashtag(Post post, Hashtag hashtag) {
        this.post = Objects.requireNonNull(post, "Post must not be null");
        this.hashtag = Objects.requireNonNull(hashtag, "Hashtag must not be null");
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
