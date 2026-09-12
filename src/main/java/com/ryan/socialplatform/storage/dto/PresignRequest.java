package com.ryan.socialplatform.storage.dto;

import com.ryan.socialplatform.storage.enums.MediaFolder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PresignRequest(
        @NotBlank(message = "File name must not be blank")
        String fileName,

        @NotBlank(message = "Content type must not be blank")
        String contentType,

        @NotNull(message = "Folder is required")
        MediaFolder folder
) {
}
