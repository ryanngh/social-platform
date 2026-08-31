package com.ryan.socialplatform.relationship.dto;

import java.util.UUID;

public record FriendshipStatusResponse(
        UUID targetUserId,
        String status,
        UUID requestId
) {
    public static FriendshipStatusResponse none(UUID targetUserId) {
        return new FriendshipStatusResponse(targetUserId, "NONE", null);
    }

    public static FriendshipStatusResponse pendingSent(UUID targetUserId, UUID requestId) {
        return new FriendshipStatusResponse(targetUserId, "PENDING_SENT", requestId);
    }

    public static FriendshipStatusResponse pendingReceived(UUID targetUserId, UUID requestId) {
        return new FriendshipStatusResponse(targetUserId, "PENDING_RECEIVED", requestId);
    }

    public static FriendshipStatusResponse friends(UUID targetUserId) {
        return new FriendshipStatusResponse(targetUserId, "FRIENDS", null);
    }
}
