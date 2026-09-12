package com.ryan.socialplatform.storage.dto;

/**
 * Response chứa thông tin presigned URL để client upload trực tiếp lên MinIO.
 *
 * @param uploadUrl   Presigned PUT URL — client dùng URL này để PUT file binary trực tiếp lên MinIO
 * @param objectKey   Object key trên MinIO (vd: "posts/{userId}/{uuid}.jpg")
 * @param publicUrl   URL công khai để truy cập file sau khi upload (dùng làm mediaUrl khi tạo post/comment)
 * @param expiresIn   Số giây trước khi presigned URL hết hạn
 */
public record PresignResponse(
        String uploadUrl,
        String objectKey,
        String publicUrl,
        int expiresIn
) {
}
