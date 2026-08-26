package com.ryan.socialplatform.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_credentials")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCredentials {

    public static final short MAX_FAILED_ATTEMPTS = 5;
    public static final Duration DEFAULT_LOCK_DURATION =
            Duration.ofMinutes(15);

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_user_credentials_user_id"
            )
    )
    private User user;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone_number", nullable = false, length = 20)
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

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    public UserCredentials(
            User user,
            String phoneNumber,
            String passwordHash
    ) {
        this.user = user;
        this.phoneNumber = phoneNumber;
        this.passwordHash = passwordHash;
    }

    public boolean isAccountLocked() {
        return lockedUntil != null
                && Instant.now().isBefore(lockedUntil);
    }

    public void recordFailedLogin(
            int maxAttempts,
            Duration lockDuration
    ) {
        this.failedLoginCount++;

        if (this.failedLoginCount >= maxAttempts) {
            this.lockedUntil =
                    Instant.now().plus(lockDuration);
        }
    }

    public void recordFailedLogin() {
        recordFailedLogin(
                MAX_FAILED_ATTEMPTS,
                DEFAULT_LOCK_DURATION
        );
    }

    public void recordSuccessfulLogin() {
        this.failedLoginCount = 0;
        this.lockedUntil = null;
    }

    public void unlock() {
        this.failedLoginCount = 0;
        this.lockedUntil = null;
    }

    public boolean canLogin() {
        return !this.deleted && !isAccountLocked();
    }
}