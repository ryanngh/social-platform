package com.ryan.socialplatform.comment.dto;

import com.ryan.socialplatform.user.dto.UserSummaryResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UUID postId,
        UUID parentCommentId,
        UserSummaryResponse author,
        String content,
        List<CommentMediaResponse> media,
        List<UserSummaryResponse> mentions,
        int likeCount,
        int replyCount,
        boolean isPinned,
        Instant pinnedAt,
        Instant createdAt,
        Instant editedAt,
        Instant updatedAt
) {
    public static CommentResponse of(
            UUID id,
            UUID postId,
            UUID parentCommentId,
            UserSummaryResponse author,
            String content,
            List<CommentMediaResponse> media,
            List<UserSummaryResponse> mentions,
            int likeCount,
            int replyCount,
            boolean isPinned,
            Instant pinnedAt,
            Instant createdAt,
            Instant editedAt,
            Instant updatedAt
    ) {
        return new CommentResponse(
                id,
                postId,
                parentCommentId,
                author,
                content,
                media != null ? media : List.of(),
                mentions != null ? mentions : List.of(),
                likeCount,
                replyCount,
                isPinned,
                pinnedAt,
                createdAt,
                editedAt,
                updatedAt
        );
    }
}