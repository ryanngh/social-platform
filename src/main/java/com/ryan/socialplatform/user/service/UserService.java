package com.ryan.socialplatform.user.service;

import com.ryan.socialplatform.auth.repository.UserCredentialsRepository;
import com.ryan.socialplatform.auth.repository.UserSessionRepository;
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

    public UserService(UserRepository userRepository,
                       UserProfileRepository userProfileRepository,
                       UserCredentialsRepository userCredentialsRepository,
                       UserAppRoleRepository userAppRoleRepository,
                       UserSessionRepository userSessionRepository) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userCredentialsRepository = userCredentialsRepository;
        this.userAppRoleRepository = userAppRoleRepository;
        this.userSessionRepository = userSessionRepository;
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
}