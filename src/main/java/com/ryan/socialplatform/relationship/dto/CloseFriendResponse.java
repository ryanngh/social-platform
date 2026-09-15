package com.ryan.socialplatform.relationship.dto;

import com.ryan.socialplatform.relationship.entity.CloseFriend;
import com.ryan.socialplatform.user.dto.UserSummaryResponse;
import com.ryan.socialplatform.user.entity.UserProfile;

import java.time.Instant;
import java.util.UUID;

public record CloseFriendResponse(
        UUID id,
        UserSummaryResponse friend,
        Instant addedAt
) {
    public static CloseFriendResponse of(CloseFriend closeFriend, UserProfile friendProfile) {
        return new CloseFriendResponse(
                closeFriend.getId(),
                UserSummaryResponse.from(friendProfile),
                closeFriend.getCreatedAt()
        );
    }
}