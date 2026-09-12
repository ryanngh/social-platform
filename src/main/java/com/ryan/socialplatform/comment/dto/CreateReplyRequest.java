package com.ryan.socialplatform.comment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateReplyRequest(
        @Size(max = 10000, message = "Reply content must not exceed 10000 characters")
        String content,

        @Size(max = 5, message = "Cannot attach more than 5 media items per reply")
        @Valid
        List<CommentMediaRequest> media,

        @Size(max = 20, message = "Cannot mention more than 20 users per reply")
        List<UUID> mentionedUserIds
) {
    public CreateReplyRequest {
        if (media == null) {
            media = List.of();
        }
        if (mentionedUserIds == null) {
            mentionedUserIds = List.of();
        }
    }
}
