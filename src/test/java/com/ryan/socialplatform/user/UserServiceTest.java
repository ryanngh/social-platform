package com.ryan.socialplatform.user;

import com.ryan.socialplatform.auth.exceptions.AccountAlreadyExistsException;
import com.ryan.socialplatform.auth.repository.UserCredentialsRepository;
import com.ryan.socialplatform.auth.repository.UserSessionRepository;
import com.ryan.socialplatform.storage.StorageService;
import com.ryan.socialplatform.user.dto.UserProfileUpdateRequest;
import com.ryan.socialplatform.user.dto.UserResponse;
import com.ryan.socialplatform.user.dto.UserSummaryResponse;
import com.ryan.socialplatform.user.entity.User;
import com.ryan.socialplatform.user.entity.UserProfile;
import com.ryan.socialplatform.user.enums.Status;
import com.ryan.socialplatform.user.exceptions.UserNotFoundException;
import com.ryan.socialplatform.user.repository.UserAppRoleRepository;
import com.ryan.socialplatform.user.repository.UserProfileRepository;
import com.ryan.socialplatform.user.repository.UserRepository;
import com.ryan.socialplatform.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserCredentialsRepository userCredentialsRepository;

    @Mock
    private UserAppRoleRepository userAppRoleRepository;

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private UserService userService;

    private UUID userId;
    private User user;
    private UserProfile profile;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.create();
        ReflectionTestUtils.setField(user, "id", userId);
        profile = new UserProfile(user, "John", "Doe", "johndoe");
        ReflectionTestUtils.setField(profile, "userId", userId);
    }

    @Test
    @DisplayName("getProfileByUsername returns UserResponse when found")
    void getProfileByUsername_Success() {
        UserResponse mockResponse = UserResponse.from(user, profile, false);

        when(userRepository.findProfileByUsernameAndViewer("johndoe", null))
                .thenReturn(Optional.of(mockResponse));

        UserResponse result = userService.getProfileByUsername("johndoe");

        assertNotNull(result);
        assertEquals("johndoe", result.username());
        assertEquals("John", result.firstName());
        assertEquals("Doe", result.lastName());
        assertEquals("John Doe", result.fullName());
    }

    @Test
    @DisplayName("getProfileByUsername throws UserNotFoundException when not found")
    void getProfileByUsername_NotFound() {
        when(userRepository.findProfileByUsernameAndViewer("unknown_user", null))
                .thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getProfileByUsername("unknown_user"));
    }

    @Test
    @DisplayName("getProfileByUsername throws IllegalArgumentException on blank username")
    void getProfileByUsername_BlankUsername() {
        assertThrows(IllegalArgumentException.class, () -> userService.getProfileByUsername("   "));
    }

    @Test
    @DisplayName("updateProfile updates username successfully")
    void updateProfile_UpdateUsername_Success() {
        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(userProfileRepository.existsByUsernameIgnoreCase("new_handle")).thenReturn(false);
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileUpdateRequest request = new UserProfileUpdateRequest(
                "new_handle", "John", "Smith", null, null, null, null, null, null, null, null
        );

        UserResponse result = userService.updateProfile(userId, request);

        assertNotNull(result);
        assertEquals("new_handle", result.username());
        assertEquals("John", result.firstName());
        assertEquals("Smith", result.lastName());
        verify(userProfileRepository).save(profile);
    }

    @Test
    @DisplayName("updateProfile throws AccountAlreadyExistsException when new username is taken")
    void updateProfile_UsernameAlreadyTaken() {
        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(userProfileRepository.existsByUsernameIgnoreCase("taken_handle")).thenReturn(true);

        UserProfileUpdateRequest request = new UserProfileUpdateRequest(
                "taken_handle", "John", "Smith", null, null, null, null, null, null, null, null
        );

        assertThrows(AccountAlreadyExistsException.class, () -> userService.updateProfile(userId, request));
        verify(userProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("UserSummaryResponse maps username correctly")
    void userSummaryResponse_Mapping() {
        UserSummaryResponse summary = UserSummaryResponse.from(profile);

        assertNotNull(summary);
        assertEquals(userId, summary.id());
        assertEquals("johndoe", summary.username());
        assertEquals("John", summary.firstName());
        assertEquals("Doe", summary.lastName());
        assertEquals("John Doe", summary.fullName());
    }
}
