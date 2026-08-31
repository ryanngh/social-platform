package com.ryan.socialplatform.relationship.dto;

import com.ryan.socialplatform.relationship.entity.UserBlock;
import com.ryan.socialplatform.user.dto.UserSummaryResponse;
import com.ryan.socialplatform.user.entity.UserProfile;

import java.time.Instant;
import java.util.UUID;

public record UserBlockResponse(
        UUID id,
        UserSummaryResponse blocked,
        Instant blockedAt
) {
    public static UserBlockResponse of(UserBlock block, UserProfile blockedProfile) {
        return new UserBlockResponse(
                block.getId(),
                UserSummaryResponse.from(blockedProfile),
                block.getCreatedAt()
        );
    }
}
