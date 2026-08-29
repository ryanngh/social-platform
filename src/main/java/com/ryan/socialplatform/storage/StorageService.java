package com.ryan.socialplatform.storage;

import io.minio.*;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Service
public class StorageService {
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