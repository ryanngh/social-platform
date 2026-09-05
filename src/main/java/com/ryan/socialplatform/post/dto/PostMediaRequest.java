package com.ryan.socialplatform.post.dto;

import com.ryan.socialplatform.post.enums.MediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PostMediaRequest(
        @NotBlank(message = "Media URL must not be blank")
        String mediaUrl,

        @NotNull(message = "Media type is required")
        MediaType mediaType,

        String thumbnailUrl
) {
}
