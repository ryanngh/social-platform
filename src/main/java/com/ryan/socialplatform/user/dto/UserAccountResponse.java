package com.ryan.socialplatform.user.dto;

import com.ryan.socialplatform.user.entity.User;
import com.ryan.socialplatform.user.entity.UserCredentials;
import com.ryan.socialplatform.user.enums.Status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserAccountResponse(
        UUID userId,
        String email,
        String phoneNumber,
        Status status,
        boolean isVerified,
        List<String> roles,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserAccountResponse of(User user, UserCredentials credentials, List<String> roles) {
        return new UserAccountResponse(
                user.getId(),
                credentials != null ? credentials.getEmail() : null,
                credentials != null ? credentials.getPhoneNumber() : null,
                user.getStatus(),
                user.isVerified(),
                roles != null ? roles : List.of(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
