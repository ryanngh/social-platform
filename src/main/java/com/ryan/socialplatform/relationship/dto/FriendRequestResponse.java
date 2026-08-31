package com.ryan.socialplatform.relationship.dto;

import com.ryan.socialplatform.relationship.entity.FriendRequest;
import com.ryan.socialplatform.relationship.enums.FriendRequestStatus;
import com.ryan.socialplatform.user.dto.UserSummaryResponse;
import com.ryan.socialplatform.user.entity.UserProfile;

import java.time.Instant;
import java.util.UUID;

public record FriendRequestResponse(
        UUID id,
        UserSummaryResponse sender,
        UserSummaryResponse receiver,
        FriendRequestStatus status,
        Instant createdAt,
        Instant respondedAt
) {
    public static FriendRequestResponse of(FriendRequest request, UserProfile senderProfile, UserProfile receiverProfile) {
        return new FriendRequestResponse(
                request.getId(),
                UserSummaryResponse.from(senderProfile),
                UserSummaryResponse.from(receiverProfile),
                request.getStatus(),
                request.getCreatedAt(),
                request.getRespondedAt()
        );
    }
}
