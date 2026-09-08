package com.ryan.socialplatform.comment.dto;

import com.ryan.socialplatform.user.dto.UserSummaryResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReplyResponse(
        UUID id,
        UUID postId,
        UUID parentCommentId,
        UserSummaryResponse author,
        UserSummaryResponse replyToUser,
        String content,
        List<CommentMediaResponse> media,
        List<UserSummaryResponse> mentions,
        int likeCount,
        Instant createdAt,
        Instant editedAt,
        Instant updatedAt
) {
    public static ReplyResponse of(
            UUID id,
            UUID postId,
            UUID parentCommentId,
            UserSummaryResponse author,
            UserSummaryResponse replyToUser,
            String content,
            List<CommentMediaResponse> media,
            List<UserSummaryResponse> mentions,
            int likeCount,
            Instant createdAt,
            Instant editedAt,
            Instant updatedAt
    ) {
        return new ReplyResponse(
                id,
                postId,
                parentCommentId,
                author,
                replyToUser,
                content,
                media != null ? media : List.of(),
                mentions != null ? mentions : List.of(),
                likeCount,
                createdAt,
                editedAt,
                updatedAt
        );
    }
}
