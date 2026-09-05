package com.ryan.socialplatform.post.dto;

import com.ryan.socialplatform.post.entity.PostMedia;
import com.ryan.socialplatform.post.enums.MediaType;

import java.util.UUID;

public record PostMediaResponse(
        UUID id,
        String mediaUrl,
        MediaType mediaType,
        String thumbnailUrl,
        short displayOrder
) {
    public static PostMediaResponse from(PostMedia media) {
        if (media == null) {
            return null;
        }
        return new PostMediaResponse(
                media.getId(),
                media.getMediaUrl(),
                media.getMediaType(),
                media.getThumbnailUrl(),
                media.getDisplayOrder()
        );
    }
}
