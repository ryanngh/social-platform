package com.ryan.socialplatform.storage.dto;

import java.util.List;

public record PresignBatchResponse(
        List<PresignResponse> uploads
) {
}
