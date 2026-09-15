package com.ryan.socialplatform.relationship.entity;

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
@Table(name = "close_friends")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CloseFriend {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_close_friends_user")
    )
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "friend_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_close_friends_friend")
    )
    private User friend;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public CloseFriend(User user, User friend) {
        this.user = Objects.requireNonNull(user, "User must not be null");
        this.friend = Objects.requireNonNull(friend, "Friend must not be null");
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}