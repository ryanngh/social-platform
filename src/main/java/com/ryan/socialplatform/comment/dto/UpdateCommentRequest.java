package com.ryan.socialplatform.comment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record UpdateCommentRequest(
        @Size(max = 10000, message = "Comment content must not exceed 10000 characters")
        String content,

        @Size(max = 5, message = "Cannot attach more than 5 media items per comment")
        @Valid
        List<CommentMediaRequest> media,

        @Size(max = 20, message = "Cannot mention more than 20 users per comment")
        List<UUID> mentionedUserIds
) {
    public UpdateCommentRequest {
        if (media == null) {
            media = List.of();
        }
        if (mentionedUserIds == null) {
            mentionedUserIds = List.of();
        }
    }
}
