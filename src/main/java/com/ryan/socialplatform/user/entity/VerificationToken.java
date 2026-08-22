package com.ryan.socialplatform.user.entity;

import com.ryan.socialplatform.user.enums.VerificationTokenType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "verification_tokens")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VerificationToken {

    @Id
    @Column(name = "token_hash", length = 64, nullable = false)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_verification_tokens_user_id")
    )
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationTokenType type;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    public VerificationToken(String tokenHash, User user, VerificationTokenType type, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.user = user;
        this.type = type;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
