package com.ryan.socialplatform.storage.dto;

import com.ryan.socialplatform.storage.enums.MediaFolder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PresignBatchRequest(
        @NotNull(message = "Folder is required")
        MediaFolder folder,

        @NotNull(message = "Files list must not be null")
        @Size(min = 1, max = 30, message = "Must have between 1 and 30 files")
        @Valid
        List<FileItem> files
) {
    public record FileItem(
            @NotNull(message = "File name must not be null")
            String fileName,

            @NotNull(message = "Content type must not be null")
            String contentType
    ) {
    }
}
