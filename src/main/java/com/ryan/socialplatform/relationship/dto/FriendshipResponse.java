package com.ryan.socialplatform.relationship.dto;

import com.ryan.socialplatform.relationship.entity.Friendship;
import com.ryan.socialplatform.user.dto.UserSummaryResponse;
import com.ryan.socialplatform.user.entity.UserProfile;

import java.time.Instant;
import java.util.UUID;

public record FriendshipResponse(
        UUID id,
        UserSummaryResponse friend,
        Instant since
) {
    public static FriendshipResponse of(Friendship friendship, UserProfile friendProfile) {
        return new FriendshipResponse(
                friendship.getId(),
                UserSummaryResponse.from(friendProfile),
                friendship.getCreatedAt()
        );
    }
}
