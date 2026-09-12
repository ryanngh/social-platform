package com.ryan.socialplatform.storage;

import com.ryan.socialplatform.storage.dto.PresignResponse;
import io.minio.*;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class StorageService {
    private static final int PRESIGN_EXPIRY_SECONDS = 600; // 10 phút

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif",
            "video/mp4", "video/quicktime", "video/webm"
    );

    private final MinioClient minioClient;
    private final MinioProperties props;
    public StorageService(MinioClient minioClient, MinioProperties props) {
        this.minioClient = minioClient;
        this.props = props;
    }
    /**
     * Tự động kiểm tra và tạo bucket với quyền Public Read khi ứng dụng khởi động
     */
    @PostConstruct
    public void init() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(props.getBucket()).build()
            );
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(props.getBucket()).build()
                );
                // Set policy cho phép Public Read để hiển thị ảnh
                String policy = """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": {"AWS": ["*"]},
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """.formatted(props.getBucket());
                minioClient.setBucketPolicy(
                        SetBucketPolicyArgs.builder()
                                .bucket(props.getBucket())
                                .config(policy)
                                .build()
                );
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
    /**
     * Upload file và trả về đường dẫn URL công khai
     * @param folder Thư mục lưu trữ (vd: "avatars", "banners", "posts")
     * @param file File tải lên từ client
     * @return URL truy cập file công khai
     */
    public String uploadFile(String folder, UUID byUser,MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot null!");
        }
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        // Tên file duy nhất: folder/{ByUserId}/UUID.ext (vd: avatars/123e4567-e89b-12d3.jpg)
        String objectName = folder + "/" + byUser + "/" + UUID.randomUUID() + extension;
        // Xác định Content-Type nếu client gửi lên rỗng hoặc chung chung (octet-stream)
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank() || contentType.equalsIgnoreCase("application/octet-stream")) {
            contentType = determineContentType(extension);
        }

        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(props.getBucket())
                            .object(objectName)
                            .stream(is, file.getSize(), -1L)
                            .contentType(contentType)
                            .build()
            );
            // Trả về objectName (format: folder/{byUser}/{UUID}.ext)
            return objectName;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi upload file lên MinIO: " + e.getMessage(), e);
        }
    }

    private String determineContentType(String extension) {
        if (extension == null || extension.isBlank()) {
            return "application/octet-stream";
        }
        String cleanExt = extension.startsWith(".") ? extension.substring(1).toLowerCase() : extension.toLowerCase();
        return switch (cleanExt) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "gif" -> "image/gif";
            default -> "application/octet-stream";
        };
    }
    /**
     * Tạo Presigned PUT URL để client upload file trực tiếp lên MinIO (không qua backend).
     *
     * @param folder      Thư mục lưu trữ (vd: "posts", "comments")
     * @param userId      ID của user đang upload
     * @param fileName    Tên file gốc (dùng để lấy extension)
     * @param contentType MIME type của file (vd: "image/jpeg", "video/mp4")
     * @return PresignResponse chứa uploadUrl, objectKey, publicUrl, expiresIn
     */
    public PresignResponse generatePresignedPutUrl(String folder, UUID userId, String fileName, String contentType) {
        validateContentType(contentType);

        String extension = extractExtension(fileName);
        String objectKey = folder + "/" + userId + "/" + UUID.randomUUID() + extension;

        try {
            String uploadUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Http.Method.PUT)
                            .bucket(props.getBucket())
                            .object(objectKey)
                            .expiry(PRESIGN_EXPIRY_SECONDS, TimeUnit.SECONDS)
                            .build()
            );

            return new PresignResponse(uploadUrl, objectKey, objectKey, PRESIGN_EXPIRY_SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo presigned URL: " + e.getMessage(), e);
        }
    }

    private String extractExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    private void validateContentType(String contentType) {
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Content type không được hỗ trợ: " + contentType
                            + ". Chỉ chấp nhận: " + ALLOWED_CONTENT_TYPES
            );
        }
    }

    /**
     * Xóa file trên MinIO theo URL hoặc Object Name
     */
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }
        try {
            // Tách objectName nếu là full URL, hoặc giữ nguyên nếu là objectName
            String prefix = props.getEndpoint() + "/" + props.getBucket() + "/";
            String objectName = fileUrl.startsWith(prefix) ? fileUrl.substring(prefix.length()) : fileUrl;
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(props.getBucket())
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            // Ghi log cảnh báo nếu xóa lỗi, không làm gián đoạn luồng chính
            System.err.println("Không thể xóa file MinIO: " + e.getMessage());
        }
    }
}