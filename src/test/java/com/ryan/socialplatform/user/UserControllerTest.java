package com.ryan.socialplatform.user;

import com.ryan.socialplatform.user.dto.UserResponse;
import com.ryan.socialplatform.user.enums.Status;
import com.ryan.socialplatform.user.service.UserService;
import com.ryan.socialplatform.user.entity.User;
import com.ryan.socialplatform.user.entity.UserProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @Test
    @DisplayName("getProfile with UUID identifier calls userService.getProfile(UUID)")
    void getProfile_WithUUID() {
        UUID userId = UUID.randomUUID();
        UUID viewerId = UUID.randomUUID();
        User user = User.create();
        ReflectionTestUtils.setField(user, "id", userId);
        UserProfile profile = new UserProfile(user, "Ryan", "N", "ryan_user");
        UserResponse response = UserResponse.from(user, profile, false);

        when(userService.getProfile(userId, viewerId)).thenReturn(response);

        ResponseEntity<UserResponse> result = userController.getProfile(userId.toString(), viewerId);

        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
        assertEquals("ryan_user", result.getBody().username());
        verify(userService).getProfile(userId, viewerId);
        verify(userService, never()).getProfileByUsername(anyString(), any());
    }

    @Test
    @DisplayName("getProfile with username identifier calls userService.getProfileByUsername(String)")
    void getProfile_WithUsername() {
        UUID userId = UUID.randomUUID();
        UUID viewerId = UUID.randomUUID();
        User user = User.create();
        ReflectionTestUtils.setField(user, "id", userId);
        UserProfile profile = new UserProfile(user, "John", "Doe", "johndoe");
        UserResponse response = UserResponse.from(user, profile, false);

        when(userService.getProfileByUsername("johndoe", viewerId)).thenReturn(response);

        ResponseEntity<UserResponse> result = userController.getProfile("johndoe", viewerId);

        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
        assertEquals("johndoe", result.getBody().username());
        verify(userService).getProfileByUsername("johndoe", viewerId);
        verify(userService, never()).getProfile(any(UUID.class), any());
    }
}
