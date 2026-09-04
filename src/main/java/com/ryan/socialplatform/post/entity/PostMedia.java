package com.ryan.socialplatform.post.entity;

import com.ryan.socialplatform.post.enums.MediaType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "post_media")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "post_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_post_media_post")
    )
    private Post post;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 20)
    private MediaType mediaType;

    @Column(name = "media_url", nullable = false, columnDefinition = "TEXT")
    private String mediaUrl;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Column(name = "width")
    private Short width;

    @Column(name = "height")
    private Short height;

    @Column(name = "duration_seconds")
    private Short durationSeconds;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "display_order", nullable = false)
    private short displayOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PostMedia(Post post, MediaType mediaType, String mediaUrl, short displayOrder) {
        this.post = Objects.requireNonNull(post, "Post must not be null");
        this.mediaType = Objects.requireNonNull(mediaType, "MediaType must not be null");
        this.mediaUrl = Objects.requireNonNull(mediaUrl, "Media URL must not be null");
        this.displayOrder = displayOrder;
    }

    public PostMedia(Post post, MediaType mediaType, String mediaUrl) {
        this(post, mediaType, mediaUrl, (short) 0);
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
