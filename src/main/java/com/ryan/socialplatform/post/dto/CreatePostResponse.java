package com.ryan.socialplatform.post.dto;

import com.ryan.socialplatform.post.entity.Post;
import com.ryan.socialplatform.post.enums.PostVisibility;
import com.ryan.socialplatform.user.dto.UserSummaryResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreatePostResponse(
        UUID id,
        UserSummaryResponse author,
        String content,
        PostVisibility visibility,
        List<PostMediaResponse> media,
        List<String> hashtags,
        List<UserSummaryResponse> taggedUsers,
        Instant createdAt,
        Instant updatedAt
) {
    public static CreatePostResponse of(
            Post post,
            UserSummaryResponse author,
            List<PostMediaResponse> media,
            List<String> hashtags,
            List<UserSummaryResponse> taggedUsers
    ) {
        return new CreatePostResponse(
                post.getId(),
                author,
                post.getContent(),
                post.getVisibility(),
                media != null ? media : List.of(),
                hashtags != null ? hashtags : List.of(),
                taggedUsers != null ? taggedUsers : List.of(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}