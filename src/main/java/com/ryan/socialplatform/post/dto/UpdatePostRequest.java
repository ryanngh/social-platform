package com.ryan.socialplatform.post.dto;

import com.ryan.socialplatform.post.enums.PostVisibility;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record UpdatePostRequest(
        @Size(max = 5000, message = "Post content must not exceed 5000 characters")
        String content,

        PostVisibility visibility,

        @Size(max = 30, message = "Cannot add more than 30 hashtags")
        List<String> hashtags,

        @Size(max = 50, message = "Cannot tag more than 50 users")
        List<UUID> taggedUserIds
) {
    public UpdatePostRequest {
        if (hashtags == null) {
            hashtags = List.of();
        }
        if (taggedUserIds == null) {
            taggedUserIds = List.of();
        }
    }
}