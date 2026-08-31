package com.ryan.socialplatform.relationship.entity;

import com.ryan.socialplatform.relationship.enums.FriendRequestStatus;
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
@Table(
        name = "friend_requests",
        indexes = {
                @Index(name = "idx_friend_requests_receiver_status", columnList = "receiver_id, status"),
                @Index(name = "idx_friend_requests_sender", columnList = "sender_id")
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FriendRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "sender_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_friend_requests_sender")
    )
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "receiver_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_friend_requests_receiver")
    )
    private User receiver;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FriendRequestStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    public FriendRequest(User sender, User receiver) {
        this.sender = Objects.requireNonNull(sender, "Sender must not be null");
        this.receiver = Objects.requireNonNull(receiver, "Receiver must not be null");
        this.status = FriendRequestStatus.PENDING;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.status == null) {
            this.status = FriendRequestStatus.PENDING;
        }
    }

    public void accept() {
        this.status = FriendRequestStatus.ACCEPTED;
        this.respondedAt = Instant.now();
    }

    public void decline() {
        this.status = FriendRequestStatus.DECLINED;
        this.respondedAt = Instant.now();
    }

    public void cancel() {
        this.status = FriendRequestStatus.CANCELLED;
        this.respondedAt = Instant.now();
    }

    public void resend(User sender, User receiver) {
        this.sender = sender;
        this.receiver = receiver;
        this.status = FriendRequestStatus.PENDING;
        this.createdAt = Instant.now();
        this.respondedAt = null;
    }
}