package com.ryan.socialplatform.comment.entity;

import com.ryan.socialplatform.comment.enums.CommentMediaType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "comment_media")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "comment_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_comment_media_comment")
    )
    private Comment comment;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 20)
    private CommentMediaType mediaType;

    @Column(name = "media_url", nullable = false, columnDefinition = "TEXT")
    private String mediaUrl;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "display_order", nullable = false)
    private short displayOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public CommentMedia(Comment comment, CommentMediaType mediaType, String mediaUrl, short displayOrder) {
        this.comment = Objects.requireNonNull(comment, "Comment must not be null");
        this.mediaType = Objects.requireNonNull(mediaType, "Media type must not be null");
        this.mediaUrl = Objects.requireNonNull(mediaUrl, "Media URL must not be null");
        this.displayOrder = displayOrder;
    }

    public CommentMedia(Comment comment, CommentMediaType mediaType, String mediaUrl) {
        this(comment, mediaType, mediaUrl, (short) 0);
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
