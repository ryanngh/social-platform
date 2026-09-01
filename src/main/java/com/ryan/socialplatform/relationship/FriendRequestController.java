package com.ryan.socialplatform.relationship;

import com.ryan.socialplatform.relationship.dto.*;
import com.ryan.socialplatform.relationship.service.FriendRequestService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/friend-requests")
public class FriendRequestController {

    private final FriendRequestService friendRequestService;

    public FriendRequestController(FriendRequestService friendRequestService) {
        this.friendRequestService = friendRequestService;
    }

    // -------------------------------------------------------------------------
    // Friend Requests
    // -------------------------------------------------------------------------

    /**
     * Gửi lời mời kết bạn.
     * POST /friend-requests
     */
    @PostMapping
    public ResponseEntity<FriendRequestResponse> send(
            @Valid @RequestBody SendFriendRequest request,
            @AuthenticationPrincipal UUID currentUserId
    ) {
        return ResponseEntity.ok(friendRequestService.send(request.receiverId(), currentUserId));
    }

    /**
     * Hủy lời mời kết bạn đã gửi.
     * DELETE /friend-requests/{requestId}
     */
    @DeleteMapping("/{requestId}")
    public ResponseEntity<Void> cancel(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal UUID currentUserId
    ) {
        friendRequestService.cancel(requestId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Chấp nhận lời mời kết bạn.
     * POST /friend-requests/{requestId}/accept
     */
    @PostMapping("/{requestId}/accept")
    public ResponseEntity<FriendRequestResponse> accept(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal UUID currentUserId
    ) {
        return ResponseEntity.ok(friendRequestService.accept(requestId, currentUserId));
    }

    /**
     * Từ chối lời mời kết bạn.
     * POST /friend-requests/{requestId}/decline
     */
    @PostMapping("/{requestId}/decline")
    public ResponseEntity<FriendRequestResponse> decline(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal UUID currentUserId
    ) {
        return ResponseEntity.ok(friendRequestService.decline(requestId, currentUserId));
    }

    /**
     * Lấy danh sách lời mời đang chờ phản hồi (nhận được).
     * GET /friend-requests/received
     */
    @GetMapping("/received")
    public ResponseEntity<Page<FriendRequestResponse>> getReceivedPending(
            @AuthenticationPrincipal UUID currentUserId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(friendRequestService.getReceivedPending(currentUserId, pageable));
    }

    /**
     * Lấy danh sách lời mời đã gửi đang chờ phản hồi.
     * GET /friend-requests/sent
     */
    @GetMapping("/sent")
    public ResponseEntity<Page<FriendRequestResponse>> getSentPending(
            @AuthenticationPrincipal UUID currentUserId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(friendRequestService.getSentPending(currentUserId, pageable));
    }

    // -------------------------------------------------------------------------
    // Friendships
    // -------------------------------------------------------------------------

    /**
     * Hủy kết bạn (Unfriend).
     * DELETE /friend-requests/friends/{targetUserId}
     */
    @DeleteMapping("/friends/{targetUserId}")
    public ResponseEntity<Void> unfriend(
            @PathVariable UUID targetUserId,
            @AuthenticationPrincipal UUID currentUserId
    ) {
        friendRequestService.unfriend(targetUserId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Lấy danh sách bạn bè của mình.
     * GET /friend-requests/friends
     */
    @GetMapping("/friends")
    public ResponseEntity<Page<FriendshipResponse>> getMyFriends(
            @AuthenticationPrincipal UUID currentUserId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(friendRequestService.getFriends(currentUserId, pageable));
    }

    /**
     * Lấy danh sách bạn bè của người khác.
     * GET /friend-requests/friends/{userId}
     */
    @GetMapping("/friends/{userId}")
    public ResponseEntity<Page<FriendshipResponse>> getUserFriends(
            @PathVariable UUID userId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(friendRequestService.getFriends(userId, pageable));
    }

    /**
     * Lấy danh sách bạn chung với người khác.
     * GET /friend-requests/friends/{targetUserId}/mutual
     */
    @GetMapping("/friends/{targetUserId}/mutual")
    public ResponseEntity<Page<FriendshipResponse>> getMutualFriends(
            @PathVariable UUID targetUserId,
            @AuthenticationPrincipal UUID currentUserId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(friendRequestService.getMutualFriends(targetUserId, currentUserId, pageable));
    }

    // -------------------------------------------------------------------------
    // Blocks
    // -------------------------------------------------------------------------

    /**
     * Chặn người dùng.
     * POST /friend-requests/blocks/{targetUserId}
     */
    @PostMapping("/blocks/{targetUserId}")
    public ResponseEntity<UserBlockResponse> block(
            @PathVariable UUID targetUserId,
            @AuthenticationPrincipal UUID currentUserId
    ) {
        return ResponseEntity.ok(friendRequestService.block(targetUserId, currentUserId));
    }

    /**
     * Bỏ chặn người dùng.
     * DELETE /friend-requests/blocks/{targetUserId}
     */
    @DeleteMapping("/blocks/{targetUserId}")
    public ResponseEntity<Void> unblock(
            @PathVariable UUID targetUserId,
            @AuthenticationPrincipal UUID currentUserId
    ) {
        friendRequestService.unblock(targetUserId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Danh sách những người mình đã chặn.
     * GET /friend-requests/blocks
     */
    @GetMapping("/blocks")
    public ResponseEntity<Page<UserBlockResponse>> getBlockedUsers(
            @AuthenticationPrincipal UUID currentUserId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(friendRequestService.getBlockedUsers(currentUserId, pageable));
    }
}
