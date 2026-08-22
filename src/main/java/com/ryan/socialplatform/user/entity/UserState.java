package com.ryan.socialplatform.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_state")
@Setter
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserState {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_state_user_id")
    )
    private User user;

    @Column(nullable = false, length = 10)
    private String presence = "OFFLINE";

    @Column(name = "custom_status_text", length = 128)
    private String customStatusText;

    @Column(name = "custom_status_emoji", length = 50)
    private String customStatusEmoji;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "active_device", length = 10)
    private String activeDevice;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UserState(User user) {
        this.user = user;
        this.userId = user.getId();
    }

    @PrePersist
    protected void onCreate() {
        if (this.presence == null) {
            this.presence = "OFFLINE";
        }
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}