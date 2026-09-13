package com.ryan.socialplatform.relationship.service;

import com.ryan.socialplatform.common.exception.BadRequestException;
import com.ryan.socialplatform.common.exception.ConflictException;
import com.ryan.socialplatform.common.exception.ForbiddenException;
import com.ryan.socialplatform.common.exception.ResourceNotFoundException;
import com.ryan.socialplatform.relationship.dto.*;
import com.ryan.socialplatform.relationship.entity.FriendRequest;
import com.ryan.socialplatform.relationship.entity.Friendship;
import com.ryan.socialplatform.relationship.entity.UserBlock;
import com.ryan.socialplatform.relationship.enums.FriendRequestStatus;
import com.ryan.socialplatform.relationship.exceptions.FriendRequestNotFoundException;
import com.ryan.socialplatform.relationship.repository.FriendRequestRepository;
import com.ryan.socialplatform.relationship.repository.FriendshipRepository;
import com.ryan.socialplatform.relationship.repository.UserBlockRepository;
import com.ryan.socialplatform.user.entity.User;
import com.ryan.socialplatform.user.entity.UserProfile;
import com.ryan.socialplatform.user.exceptions.UserNotFoundException;
import com.ryan.socialplatform.user.repository.UserProfileRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FriendRequestService {

    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserBlockRepository userBlockRepository;
    private final UserProfileRepository userProfileRepository;

    public FriendRequestService(FriendRequestRepository friendRequestRepository,
                                FriendshipRepository friendshipRepository,
                                UserBlockRepository userBlockRepository,
                                UserProfileRepository userProfileRepository) {
        this.friendRequestRepository = friendRequestRepository;
        this.friendshipRepository = friendshipRepository;
        this.userBlockRepository = userBlockRepository;
        this.userProfileRepository = userProfileRepository;
    }

    // -------------------------------------------------------------------------
    // Friend Requests
    // -------------------------------------------------------------------------

    /**
     * Gửi lời mời kết bạn.
     */
    @Transactional
    public FriendRequestResponse send(UUID targetUserId, UUID currentUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new BadRequestException("Cannot send a friend request to yourself");
        }
        if (userBlockRepository.existsBlockBetween(currentUserId, targetUserId)) {
            throw new ForbiddenException("Cannot send a friend request to this user");
        }
        if (friendshipRepository.areFriends(currentUserId, targetUserId)) {
            throw new ConflictException("You are already friends with this user");
        }

        UserProfile senderProfile = userProfileRepository.findById(currentUserId)
                .orElseThrow(UserNotFoundException::new);
        UserProfile receiverProfile = userProfileRepository.findById(targetUserId)
                .orElseThrow(UserNotFoundException::new);
        User sender = senderProfile.getUser();
        User receiver = receiverProfile.getUser();

        Optional<FriendRequest> existingOpt = friendRequestRepository.findBetweenUsers(currentUserId, targetUserId);

        FriendRequest request;
        if (existingOpt.isEmpty()) {
            request = new FriendRequest(sender, receiver);
        } else {
            FriendRequest existing = existingOpt.get();
            switch (existing.getStatus()) {
               //  case ACCEPTED -> throw new IllegalStateException("You are already friends with this user"); -> Case này bị sai
                case PENDING -> {
                    if (existing.getSender().getId().equals(currentUserId)) {
                        throw new ConflictException("You have already sent a friend request to this user");
                    } else {
                        throw new ConflictException("This user has already sent you a friend request. Accept or decline it instead");
                    }
                }
                case ACCEPTED, DECLINED, CANCELLED -> {
                    existing.resend(sender, receiver);
                    request = existing;
                }
                default -> throw new IllegalStateException("Unexpected friend request status");
            }
        }

        try {
            FriendRequest saved = friendRequestRepository.saveAndFlush(request);
            return FriendRequestResponse.of(saved, senderProfile, receiverProfile);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("A friend request already exists between you and this user");
        }
    }

    /**
     * Hủy lời mời kết bạn đã gửi.
     */
    @Transactional
    public void cancel(UUID requestId, UUID currentUserId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(FriendRequestNotFoundException::new);

        if (!request.getSender().getId().equals(currentUserId)) {
            throw new ForbiddenException("You can only cancel your own friend requests");
        }
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new ConflictException("Only pending requests can be cancelled");
        }
        request.cancel();
    }

    /**
     * Chấp nhận lời mời kết bạn.
     * Tạo bản ghi trong bảng friendships.
     */
    @Transactional
    public FriendRequestResponse accept(UUID requestId, UUID currentUserId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(FriendRequestNotFoundException::new);

        if (!request.getReceiver().getId().equals(currentUserId)) {
            throw new ForbiddenException("You can only accept requests sent to you");
        }
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new ConflictException("Only pending requests can be accepted");
        }

        request.accept();

        // Tạo bản ghi friendship chính thức
        Friendship friendship = new Friendship(request.getSender(), request.getReceiver());
        friendshipRepository.save(friendship);

        UserProfile senderProfile = userProfileRepository.findById(request.getSender().getId())
                .orElseThrow(UserNotFoundException::new);
        UserProfile receiverProfile = userProfileRepository.findById(currentUserId)
                .orElseThrow(UserNotFoundException::new);

        return FriendRequestResponse.of(request, senderProfile, receiverProfile);
    }

    /**
     * Từ chối lời mời kết bạn.
     */
    @Transactional
    public FriendRequestResponse decline(UUID requestId, UUID currentUserId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(FriendRequestNotFoundException::new);

        if (!request.getReceiver().getId().equals(currentUserId)) {
            throw new ForbiddenException("You can only decline requests sent to you");
        }
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new ConflictException("Only pending requests can be declined");
        }

        request.decline();

        UserProfile senderProfile = userProfileRepository.findById(request.getSender().getId())
                .orElseThrow(UserNotFoundException::new);
        UserProfile receiverProfile = userProfileRepository.findById(currentUserId)
                .orElseThrow(UserNotFoundException::new);

        return FriendRequestResponse.of(request, senderProfile, receiverProfile);
    }

    /**
     * Lấy danh sách lời mời đang chờ phản hồi (nhận được).
     */
    @Transactional(readOnly = true)
    public Page<FriendRequestResponse> getReceivedPending(UUID currentUserId, Pageable pageable) {
        Page<FriendRequest> requests = friendRequestRepository
                .findByReceiverIdAndStatus(currentUserId, FriendRequestStatus.PENDING, pageable);

        return mapRequestsToResponse(requests, currentUserId);
    }

    /**
     * Lấy danh sách lời mời đã gửi đang chờ phản hồi.
     */
    @Transactional(readOnly = true)
    public Page<FriendRequestResponse> getSentPending(UUID currentUserId, Pageable pageable) {
        Page<FriendRequest> requests = friendRequestRepository
                .findBySenderIdAndStatus(currentUserId, FriendRequestStatus.PENDING, pageable);

        return mapRequestsToResponse(requests, currentUserId);
    }

    // -------------------------------------------------------------------------
    // Friendships
    // -------------------------------------------------------------------------

    /**
     * Hủy kết bạn (Unfriend).
     */
    @Transactional
    public void unfriend(UUID targetUserId, UUID currentUserId) {
        Friendship friendship = friendshipRepository.findBetweenUsers(currentUserId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("You are not friends with this user"));
        friendshipRepository.delete(friendship);
    }

    /**
     * Lấy danh sách bạn bè (phân trang).
     */
    @Transactional(readOnly = true)
    public Page<FriendshipResponse> getFriends(UUID userId, Pageable pageable) {
        Page<Friendship> friendships = friendshipRepository.findAllByUserId(userId, pageable);

        // Lấy tất cả profile cần thiết trong 1 lần query (IN clause)
        List<UUID> friendIds = friendships.stream()
                .map(f -> f.getUser1().getId().equals(userId) ? f.getUser2().getId() : f.getUser1().getId())
                .toList();

        Map<UUID, UserProfile> profileMap = userProfileRepository.findAllById(friendIds)
                .stream().collect(Collectors.toMap(UserProfile::getUserId, Function.identity()));

        return friendships.map(f -> {
            UUID friendId = f.getUser1().getId().equals(userId) ? f.getUser2().getId() : f.getUser1().getId();
            return FriendshipResponse.of(f, profileMap.get(friendId));
        });
    }

    /**
     * Lấy danh sách bạn chung với 1 người (phân trang).
     */
    @Transactional(readOnly = true)
    public Page<FriendshipResponse> getMutualFriends(UUID targetUserId, UUID currentUserId, Pageable pageable) {
        Page<Friendship> mutuals = friendshipRepository.findMutualFriends(currentUserId, targetUserId, pageable);

        List<UUID> mutualIds = mutuals.stream()
                .map(f -> f.getUser1().getId().equals(currentUserId) ? f.getUser2().getId() : f.getUser1().getId())
                .toList();

        Map<UUID, UserProfile> profileMap = userProfileRepository.findAllById(mutualIds)
                .stream().collect(Collectors.toMap(UserProfile::getUserId, Function.identity()));

        return mutuals.map(f -> {
            UUID friendId = f.getUser1().getId().equals(currentUserId) ? f.getUser2().getId() : f.getUser1().getId();
            return FriendshipResponse.of(f, profileMap.get(friendId));
        });
    }

    // -------------------------------------------------------------------------
    // Blocks
    // -------------------------------------------------------------------------

    /**
     * Chặn người dùng.
     * Tự động hủy lời mời kết bạn đang chờ (nếu có) và hủy kết bạn (nếu đã là bạn bè).
     */
    @Transactional
    public UserBlockResponse block(UUID targetUserId, UUID currentUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new BadRequestException("Cannot block yourself");
        }
        if (userBlockRepository.existsByBlockerIdAndBlockedId(currentUserId, targetUserId)) {
            throw new ConflictException("You have already blocked this user");
        }

        // Hủy bất kỳ lời mời kết bạn nào đang PENDING giữa 2 người
        friendRequestRepository.findBetweenUsers(currentUserId, targetUserId)
                .filter(r -> r.getStatus() == FriendRequestStatus.PENDING)
                .ifPresent(r -> r.cancel());

        // Hủy kết bạn nếu đang là bạn bè
        friendshipRepository.findBetweenUsers(currentUserId, targetUserId)
                .ifPresent(friendshipRepository::delete);

        UserProfile blockerProfile = userProfileRepository.findById(currentUserId)
                .orElseThrow(UserNotFoundException::new);
        UserProfile blockedProfile = userProfileRepository.findById(targetUserId)
                .orElseThrow(UserNotFoundException::new);

        UserBlock block = new UserBlock(blockerProfile.getUser(), blockedProfile.getUser());
        UserBlock saved = userBlockRepository.save(block);
        return UserBlockResponse.of(saved, blockedProfile);
    }

    /**
     * Bỏ chặn người dùng.
     */
    @Transactional
    public void unblock(UUID targetUserId, UUID currentUserId) {
        UserBlock block = userBlockRepository.findByBlockerIdAndBlockedId(currentUserId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Block record not found"));
        userBlockRepository.delete(block);
    }

    /**
     * Danh sách những người mình đã chặn.
     */
    @Transactional(readOnly = true)
    public Page<UserBlockResponse> getBlockedUsers(UUID currentUserId, Pageable pageable) {
        Page<UserBlock> blocks = userBlockRepository.findAllByBlockerId(currentUserId, pageable);

        List<UUID> blockedIds = blocks.stream().map(b -> b.getBlocked().getId()).toList();
        Map<UUID, UserProfile> profileMap = userProfileRepository.findAllById(blockedIds)
                .stream().collect(Collectors.toMap(UserProfile::getUserId, Function.identity()));

        return blocks.map(b -> UserBlockResponse.of(b, profileMap.get(b.getBlocked().getId())));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Page<FriendRequestResponse> mapRequestsToResponse(Page<FriendRequest> requests, UUID currentUserId) {
        // Gom tất cả các userID cần lấy profile -> 1 query IN duy nhất
        List<UUID> userIds = requests.stream()
                .flatMap(r -> java.util.stream.Stream.of(r.getSender().getId(), r.getReceiver().getId()))
                .distinct()
                .toList();

        Map<UUID, UserProfile> profileMap = userProfileRepository.findAllById(userIds)
                .stream().collect(Collectors.toMap(UserProfile::getUserId, Function.identity()));

        return requests.map(r -> FriendRequestResponse.of(
                r,
                profileMap.get(r.getSender().getId()),
                profileMap.get(r.getReceiver().getId())
        ));
    }
}
