package com.ryan.socialplatform.user.service;

import com.ryan.socialplatform.auth.repository.UserCredentialsRepository;
import com.ryan.socialplatform.auth.repository.UserSessionRepository;
import com.ryan.socialplatform.storage.StorageService;
import com.ryan.socialplatform.user.dto.UserAccountResponse;
import com.ryan.socialplatform.user.dto.UserProfileUpdateRequest;
import com.ryan.socialplatform.user.dto.UserResponse;
import com.ryan.socialplatform.user.entity.User;
import com.ryan.socialplatform.user.entity.UserCredentials;
import com.ryan.socialplatform.user.entity.UserProfile;
import com.ryan.socialplatform.user.enums.Status;
import com.ryan.socialplatform.user.exceptions.UserNotFoundException;
import com.ryan.socialplatform.user.repository.UserAppRoleRepository;
import com.ryan.socialplatform.user.repository.UserProfileRepository;
import com.ryan.socialplatform.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserCredentialsRepository userCredentialsRepository;
    private final UserAppRoleRepository userAppRoleRepository;
    private final UserSessionRepository userSessionRepository;

    private final StorageService storageService;

    public UserService(UserRepository userRepository,
                       UserProfileRepository userProfileRepository,
                       UserCredentialsRepository userCredentialsRepository,
                       UserAppRoleRepository userAppRoleRepository,
                       UserSessionRepository userSessionRepository,
                       StorageService storageService) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userCredentialsRepository = userCredentialsRepository;
        this.userAppRoleRepository = userAppRoleRepository;
        this.userSessionRepository = userSessionRepository;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(UUID userId, UUID viewerId) {
        UserResponse response = userRepository.findProfileByIdAndViewer(userId, viewerId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (response.status() == Status.DELETED) {
            throw new UserNotFoundException(userId);
        }

        return response;
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(UUID userId) {
        return getProfile(userId, null);
    }

    @Transactional(readOnly = true)
    public UserResponse getMyProfile(UUID currentUserId) {
        return getProfile(currentUserId, currentUserId);
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UserProfileUpdateRequest request) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (profile.getUser().getStatus() == Status.DELETED) {
            throw new UserNotFoundException(userId);
        }

        // Bắt buộc — @NotBlank đảm bảo luôn có giá trị, set thẳng an toàn
        profile.setFirstName(request.firstName());
        profile.setLastName(request.lastName());

        // Optional — chỉ ghi đè khi client thật sự gửi giá trị,
        // tránh null vô tình xoá mất dữ liệu cũ (partial update)
        if (request.avatarUrl() != null) {
            profile.setAvatarUrl(request.avatarUrl());
        }
        if (request.bannerUrl() != null) {
            profile.setBannerUrl(request.bannerUrl());
        }
        if (request.bio() != null) {
            profile.setBio(request.bio());
        }
        if (request.pronouns() != null) {
            profile.setPronouns(request.pronouns());
        }
        if (request.location() != null) {
            profile.setLocation(request.location());
        }
        if (request.websiteUrl() != null) {
            profile.setWebsiteUrl(request.websiteUrl());
        }
        if (request.birthday() != null) {
            profile.setBirthday(request.birthday());
        }
        if (request.pronunciation() != null) {
            profile.setPronunciation(request.pronunciation());
        }

        UserProfile savedProfile = userProfileRepository.save(profile);

        return UserResponse.from(profile.getUser(), savedProfile, true);
    }

    @Transactional(readOnly = true)
    public UserAccountResponse getAccountInfo(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        UserCredentials credentials = userCredentialsRepository.findById(userId)
                .orElse(null);

        List<String> roles = userAppRoleRepository.findAllByUserId(userId).stream()
                .map(r -> r.getRole().name())
                .toList();

        return UserAccountResponse.of(user, credentials, roles);
    }

    @Transactional
    public UserAccountResponse deactivateAccount(UUID currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException(currentUserId));

        if (user.getStatus() != Status.ACTIVE) {
            throw new IllegalStateException("Only active accounts can be deactivated. Current status: " + user.getStatus());
        }

        user.setStatus(Status.DEACTIVATED);
        userRepository.save(user);

        userSessionRepository.revokeAllByUserId(currentUserId, Instant.now());

        return getAccountInfo(currentUserId);
    }

    @Transactional
    public void deleteAccount(UUID currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException(currentUserId));

        if (user.getStatus() == Status.DELETED) {
            return;
        }

        user.setStatus(Status.DELETED);
        userRepository.save(user);

        userCredentialsRepository.findById(currentUserId).ifPresent(credentials -> {
            credentials.setDeleted(true);
            userCredentialsRepository.save(credentials);
        });

        userSessionRepository.revokeAllByUserId(currentUserId, Instant.now());
    }

    @Transactional
    public UserAccountResponse suspendUser(UUID targetUserId, UUID operatorId) {
        if (targetUserId.equals(operatorId)) {
            throw new IllegalArgumentException("You cannot suspend your own account");
        }

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException(targetUserId));

        if (user.getStatus() == Status.DELETED) {
            throw new IllegalStateException("Cannot suspend a deleted account");
        }

        user.setStatus(Status.SUSPENDED);
        userRepository.save(user);

        userSessionRepository.revokeAllByUserId(targetUserId, Instant.now());

        return getAccountInfo(targetUserId);
    }

    @Transactional
    public UserAccountResponse reactivateUser(UUID targetUserId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException(targetUserId));

        if (user.getStatus() == Status.DELETED) {
            throw new IllegalStateException("Cannot reactivate a deleted account");
        }

        user.setStatus(Status.ACTIVE);
        userRepository.save(user);

        return getAccountInfo(targetUserId);
    }

    /**
     * Cập nhật Avatar người dùng:
     * - Validate file ảnh
     * - Upload lên MinIO folder "avatars"
     * - Xóa avatar cũ trên MinIO (nếu có)
     * - Cập nhật database và trả về UserResponse mới nhất
     */
    @Transactional
    public UserResponse updateAvatar(UUID userId, MultipartFile file) {
        validateImageFile(file);

        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (profile.getUser().getStatus() == Status.DELETED) {
            throw new UserNotFoundException(userId);
        }

        String oldAvatarUrl = profile.getAvatarUrl();

        // 1. Upload ảnh mới lên MinIO
        String newAvatarUrl = storageService.uploadFile("avatars", file);

        // 2. Cập nhật vào DB
        profile.setAvatarUrl(newAvatarUrl);
        UserProfile savedProfile = userProfileRepository.save(profile);

        // 3. Xóa avatar cũ khỏi MinIO để dọn rác
        if (oldAvatarUrl != null && !oldAvatarUrl.isBlank()) {
            storageService.deleteFile(oldAvatarUrl);
        }

        return UserResponse.from(profile.getUser(), savedProfile, true);
    }

    /**
     * Cập nhật Banner (Ảnh bìa) người dùng
     */
    @Transactional
    public UserResponse updateBanner(UUID userId, MultipartFile file) {
        validateImageFile(file);

        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (profile.getUser().getStatus() == Status.DELETED) {
            throw new UserNotFoundException(userId);
        }

        String oldBannerUrl = profile.getBannerUrl();

        // 1. Upload ảnh mới lên MinIO
        String newBannerUrl = storageService.uploadFile("banners", file);

        // 2. Cập nhật vào DB
        profile.setBannerUrl(newBannerUrl);
        UserProfile savedProfile = userProfileRepository.save(profile);

        // 3. Xóa banner cũ khỏi MinIO
        if (oldBannerUrl != null && !oldBannerUrl.isBlank()) {
            storageService.deleteFile(oldBannerUrl);
        }

        return UserResponse.from(profile.getUser(), savedProfile, true);
    }


    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File upload must not be empty.");
        }

        // Giới hạn dung lượng tối đa 25MB
        long maxSizeBytes = 25 * 1024 * 1024;
        if (file.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException("File size exceeds the allowed limit (maximum 25 MB)");
        }

        // Chỉ cho phép định dạng ảnh
        String contentType = file.getContentType();
        if (contentType == null || !(
                contentType.equalsIgnoreCase("image/jpeg") ||
                        contentType.equalsIgnoreCase("image/png") ||
                        contentType.equalsIgnoreCase("image/webp") ||
                        contentType.equalsIgnoreCase("image/gif")
        )) {
            throw new IllegalArgumentException("Only image files are allowed (JPEG, PNG, WEBP, GIF)");
        }
    }
}