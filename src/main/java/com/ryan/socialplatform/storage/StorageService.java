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
                // Set policy cho phép đọc công khai (Public Read) để hiển thị ảnh
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
    public String uploadFile(String folder ,MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot null!");
        }
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        // Tên file duy nhất: folder/UUID.ext (vd: avatars/123e4567-e89b-12d3.jpg)
        String objectName = folder + "/" + UUID.randomUUID() + extension;
        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(props.getBucket())
                            .object(objectName)
                            .stream(is, file.getSize(), -1L)
                            .contentType(file.getContentType())
                            .build()
            );
            // Trả về full URL: http://localhost:9000/social-platform/avatars/uuid.jpg
            return String.format("%s/%s/%s", props.getEndpoint(), props.getBucket(), objectName);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi upload file lên MinIO: " + e.getMessage(), e);
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
            // Tách objectName từ URL
            String prefix = props.getEndpoint() + "/" + props.getBucket() + "/";
            String objectName = fileUrl.replace(prefix, "");
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