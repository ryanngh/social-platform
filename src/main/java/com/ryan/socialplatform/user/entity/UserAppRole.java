package com.ryan.socialplatform.user.entity;

import com.ryan.socialplatform.user.enums.AppRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "user_app_roles",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_app_roles",
                columnNames = {"user_id", "role"})
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAppRole {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_app_roles_user")
    )
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "granted_by",
            foreignKey = @ForeignKey(name = "fk_user_app_roles_granted_by")
    )
    private User grantedBy;

    @Column(name = "granted_at", nullable = false, updatable = false)
    private Instant grantedAt;

    public UserAppRole(User user, AppRole role, User grantedBy) {
        this.user = user;
        this.role = role;
        this.grantedBy = grantedBy;
    }

    @PrePersist
    protected void onCreate() {
        if (this.grantedAt == null) {
            this.grantedAt = Instant.now();
        }
    }
}
