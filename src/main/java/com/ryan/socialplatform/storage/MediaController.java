package com.ryan.socialplatform.storage;

import com.ryan.socialplatform.storage.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/media")
public class MediaController {

    private final StorageService storageService;

    public MediaController(StorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * Tạo Presigned PUT URL cho 1 file.
     * Client dùng URL trả về để upload file trực tiếp lên MinIO (không qua backend).
     */
    @PostMapping("/presign")
    public ResponseEntity<PresignResponse> presign(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody PresignRequest request
    ) {
        PresignResponse response = storageService.generatePresignedPutUrl(
                request.folder().getValue(),
                userId,
                request.fileName(),
                request.contentType()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Tạo Presigned PUT URL cho nhiều file cùng lúc (batch).
     * Dùng khi tạo post với nhiều ảnh/video.
     */
    @PostMapping("/presign/batch")
    public ResponseEntity<PresignBatchResponse> presignBatch(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody PresignBatchRequest request
    ) {
        List<PresignResponse> uploads = request.files().stream()
                .map(file -> storageService.generatePresignedPutUrl(
                        request.folder().getValue(),
                        userId,
                        file.fileName(),
                        file.contentType()
                ))
                .toList();
        return ResponseEntity.ok(new PresignBatchResponse(uploads));
    }
}
