package com.ryan.socialplatform.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_credentials")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCredentials {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_credentials_user_id")
    )
    private User user;

    @Column(length = 255, unique = true)
    private String email;

    @Column(name = "phone_number", nullable = false, length = 15, unique = true)
    private String phoneNumber;

    @Column(name = "password_hash", nullable = false, columnDefinition = "TEXT")
    private String passwordHash;

    @Column(name = "mfa_enabled", nullable = false)
    private boolean mfaEnabled = false;

    @Column(name = "mfa_secret_encrypted", columnDefinition = "TEXT")
    private String mfaSecretEncrypted;

    @Column(name = "failed_login_count", nullable = false)
    private short failedLoginCount = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "phone_verified_at")
    private Instant phoneVerifiedAt;

    public UserCredentials(User user, String phoneNumber, String passwordHash) {
        this.user = user;
        this.userId = user.getId();
        this.phoneNumber = phoneNumber;
        this.passwordHash = passwordHash;
    }
}