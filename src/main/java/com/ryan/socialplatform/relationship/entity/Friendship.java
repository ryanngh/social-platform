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
@Table(name = "friendships")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id_1",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_friendships_user_1")
    )
    private User user1;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id_2",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_friendships_user_2")
    )
    private User user2;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Tạo Friendship với user_id_1 < user_id_2 (khớp với CHECK constraint ở DB).
     */
    public Friendship(User a, User b) {
        Objects.requireNonNull(a, "User A must not be null");
        Objects.requireNonNull(b, "User B must not be null");
        if (a.getId().compareTo(b.getId()) < 0) {
            this.user1 = a;
            this.user2 = b;
        } else {
            this.user1 = b;
            this.user2 = a;
        }
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
