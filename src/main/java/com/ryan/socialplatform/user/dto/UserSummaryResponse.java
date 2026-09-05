package com.ryan.socialplatform.user.dto;

import com.ryan.socialplatform.user.entity.UserProfile;

import java.util.UUID;

public record UserSummaryResponse(
        UUID id,
        String username,
        String firstName,
        String lastName,
        String fullName,
        String avatarUrl
) {
    public static UserSummaryResponse from(UserProfile profile) {
        if (profile == null) {
            return null;
        }
        String firstName = profile.getFirstName();
        String lastName = profile.getLastName();
        String fullName = (firstName != null && lastName != null)
                ? (firstName + " " + lastName).trim()
                : (firstName != null ? firstName : lastName);

        return new UserSummaryResponse(
                profile.getUserId(),
                profile.getUsername(),
                firstName,
                lastName,
                fullName,
                profile.getAvatarUrl()
        );
    }
}
