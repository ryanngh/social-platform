package com.ryan.socialplatform.relationship.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SendFriendRequest(
        @NotNull(message = "Receiver ID must not be null")
        UUID receiverId
) {
}
