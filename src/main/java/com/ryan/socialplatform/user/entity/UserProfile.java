package com.ryan.socialplatform.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "user_profile")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_profile_user_id")
    )
    private User user;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    @Column(name = "banner_url", columnDefinition = "TEXT")
    private String bannerUrl;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(length = 50)
    private String pronouns;

    @Column(length = 100)
    private String location;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Column(name = "birthday")
    private LocalDate birthday;

    @Column(length = 100)
    private String pronunciation;

    public UserProfile(User user, String firstName, String lastName) {
        this.user = user;
        this.userId = user.getId();
        this.firstName = firstName;
        this.lastName = lastName;
    }

}