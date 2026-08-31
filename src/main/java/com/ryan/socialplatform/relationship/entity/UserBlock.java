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
@Table(name = "user_blocks")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "blocker_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_blocks_blocker")
    )
    private User blocker;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "blocked_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_blocks_blocked")
    )
    private User blocked;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UserBlock(User blocker, User blocked) {
        this.blocker = Objects.requireNonNull(blocker, "Blocker must not be null");
        this.blocked = Objects.requireNonNull(blocked, "Blocked must not be null");
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
