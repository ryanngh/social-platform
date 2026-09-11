package com.ryan.socialplatform.comment.dto;

import com.ryan.socialplatform.comment.enums.CommentMediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CommentMediaRequest(
        @NotNull(message = "Media type is required (IMAGE, GIF, or VIDEO)")
        CommentMediaType mediaType,

        @NotBlank(message = "Media URL must not be blank")
        String mediaUrl,

        Integer width,
        Integer height
) {
}