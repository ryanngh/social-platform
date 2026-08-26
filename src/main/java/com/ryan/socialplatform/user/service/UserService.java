// user/service/UserService.java
package com.ryan.socialplatform.user.service;

import com.ryan.socialplatform.user.dto.UserProfileUpdateRequest;
import com.ryan.socialplatform.user.dto.UserResponse;
import com.ryan.socialplatform.user.entity.User;
import com.ryan.socialplatform.user.entity.UserProfile;
import com.ryan.socialplatform.user.exceptions.UserNotFoundException;
import com.ryan.socialplatform.user.repository.UserProfileRepository;
import com.ryan.socialplatform.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    public UserService(UserRepository userRepository, UserProfileRepository userProfileRepository) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(UUID userId) {
        return userRepository.findProfileById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UserProfileUpdateRequest request) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // Update
        profile.setFirstName(request.firstName());
        profile.setLastName(request.lastName());
        profile.setAvatarUrl(request.avatarUrl());
        profile.setBannerUrl(request.bannerUrl());
        profile.setBio(request.bio());
        profile.setPronouns(request.pronouns());
        profile.setLocation(request.location());
        profile.setWebsiteUrl(request.websiteUrl());
        profile.setBirthday(request.birthday());
        profile.setPronunciation(request.pronunciation());

        UserProfile savedProfile = userProfileRepository.save(profile);

        return UserResponse.from(profile.getUser(), savedProfile);
    }



}