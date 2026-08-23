// user/dto/UserResponse.java
package com.ryan.socialplatform.user.dto;

import com.ryan.socialplatform.user.entity.User;
import com.ryan.socialplatform.user.entity.UserProfile;
import com.ryan.socialplatform.user.enums.Status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String firstName,
        String lastName,
        String avatarUrl,
        String bannerUrl,
        String bio,
        String pronouns,
        String location,
        String websiteUrl,
        LocalDate birthday,
        String pronunciation,
        Status status,
        boolean isVerified,
        Instant createdAt,
        Instant updatedAt
) {
    public String fullName() {
        return firstName != null ? (firstName + " " + lastName).trim() : null;
    }

    public static UserResponse from(User user, UserProfile profile) {
        return new UserResponse(
                user.getId(),
                profile != null ? profile.getFirstName() : null,
                profile != null ? profile.getLastName() : null,
                profile != null ? profile.getAvatarUrl() : null,
                profile != null ? profile.getBannerUrl() : null,
                profile != null ? profile.getBio() : null,
                profile != null ? profile.getPronouns() : null,
                profile != null ? profile.getLocation() : null,
                profile != null ? profile.getWebsiteUrl() : null,
                profile != null ? profile.getBirthday() : null,
                profile != null ? profile.getPronunciation() : null,
                user.getStatus(),
                user.isVerified(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}