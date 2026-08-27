package com.ryan.socialplatform.user;

import com.ryan.socialplatform.user.dto.UserAccountResponse;
import com.ryan.socialplatform.user.dto.UserProfileUpdateRequest;
import com.ryan.socialplatform.user.dto.UserResponse;
import com.ryan.socialplatform.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Lấy hồ sơ của chính mình
     */
    @GetMapping({"/me", "/me/profile"})
    public ResponseEntity<UserResponse> getMyProfile(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(userService.getMyProfile(userId));
    }

    /**
     * Xem hồ sơ theo ID (của người khác hoặc của mình)
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getProfile(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID viewerId
    ) {
        return ResponseEntity.ok(userService.getProfile(id, viewerId));
    }

    /**
     * Cập nhật hồ sơ cá nhân
     */
    @PutMapping("/me/profile")
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UserProfileUpdateRequest request
    ) {
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }

    /**
     * Xem thông tin & trạng thái tài khoản
     */
    @GetMapping("/me/account")
    public ResponseEntity<UserAccountResponse> getAccountInfo(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(userService.getAccountInfo(userId));
    }

    /**
     * Người dùng tự vô hiệu hoá tài khoản
     */
    @PostMapping("/me/deactivate")
    public ResponseEntity<UserAccountResponse> deactivateAccount(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(userService.deactivateAccount(userId));
    }

    /**
     * Người dùng tự xoá tài khoản (Soft delete)
     */
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal UUID userId) {
        userService.deleteAccount(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Admin/Mod đình chỉ tài khoản
     */
    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<UserAccountResponse> suspendUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID operatorId
    ) {
        return ResponseEntity.ok(userService.suspendUser(id, operatorId));
    }

    /**
     * Admin/Mod khôi phục / kích hoạt lại tài khoản
     */
    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<UserAccountResponse> reactivateUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.reactivateUser(id));
    }
}
