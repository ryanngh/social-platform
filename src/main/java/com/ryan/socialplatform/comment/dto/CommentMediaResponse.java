package com.ryan.socialplatform.comment.dto;

import com.ryan.socialplatform.comment.entity.CommentMedia;
import com.ryan.socialplatform.comment.enums.CommentMediaType;

import java.util.UUID;

public record CommentMediaResponse(
        UUID id,
        CommentMediaType mediaType,
        String mediaUrl,
        Integer width,
        Integer height,
        short displayOrder
) {
    public static CommentMediaResponse from(CommentMedia media) {
        if (media == null) {
            return null;
        }
        return new CommentMediaResponse(
                media.getId(),
                media.getMediaType(),
                media.getMediaUrl(),
                media.getWidth(),
                media.getHeight(),
                media.getDisplayOrder()
        );
    }
}