package com.ryan.socialplatform.relationship.service;

import com.ryan.socialplatform.common.exception.BadRequestException;
import com.ryan.socialplatform.common.exception.ConflictException;
import com.ryan.socialplatform.common.exception.ForbiddenException;
import com.ryan.socialplatform.common.exception.ResourceNotFoundException;
import com.ryan.socialplatform.relationship.dto.CloseFriendResponse;
import com.ryan.socialplatform.relationship.entity.CloseFriend;
import com.ryan.socialplatform.relationship.repository.CloseFriendRepository;
import com.ryan.socialplatform.relationship.repository.FriendshipRepository;
import com.ryan.socialplatform.relationship.repository.UserBlockRepository;
import com.ryan.socialplatform.user.entity.User;
import com.ryan.socialplatform.user.entity.UserProfile;
import com.ryan.socialplatform.user.exceptions.UserNotFoundException;
import com.ryan.socialplatform.user.repository.UserProfileRepository;
import com.ryan.socialplatform.user.repository.UserRepository;
import com.ryan.socialplatform.user.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CloseFriendService {
    private final CloseFriendRepository closeFriendRepository;
    private final UserBlockRepository userBlockRepository;
    private final UserProfileRepository userProfileRepository;


    public CloseFriendService(CloseFriendRepository closeFriendRepository,
                              UserRepository userRepository,
                              UserBlockRepository userBlockRepository,
                              FriendshipRepository friendshipRepository, UserProfileRepository userProfileRepository) {
        this.closeFriendRepository = closeFriendRepository;
        this.userBlockRepository = userBlockRepository;
        this.userProfileRepository = userProfileRepository;
    }


    /**
     * add close friend
     */
    @Transactional
    public CloseFriendResponse add(UUID friendId, UUID currentUserId) {
        if (currentUserId.equals(friendId)) {
            throw new BadRequestException("You cannot add yourself to close friends");
        }
        // Kiểm tra block 2 chiều
        if (userBlockRepository.existsBlockBetween(currentUserId, friendId)) {
            throw new ForbiddenException("Cannot add this user to close friends");
        }
        // Kiểm tra đã có trong danh sách bạn thân chưa
        if (closeFriendRepository.existsByUserIdAndFriendId(currentUserId, friendId)) {
            throw new ConflictException("User is already in your close friends list");
        }
        // Lấy profile của 2 người
        UserProfile currentUserProfile = userProfileRepository.findById(currentUserId)
                .orElseThrow(UserNotFoundException::new);
        UserProfile friendProfile = userProfileRepository.findById(friendId)
                .orElseThrow(UserNotFoundException::new);
        CloseFriend closeFriend = new CloseFriend(currentUserProfile.getUser(), friendProfile.getUser());
        CloseFriend saved = closeFriendRepository.save(closeFriend);
        return CloseFriendResponse.of(saved, friendProfile);
    }

    /**
     * xóa
     */
    @Transactional
    public void remove(UUID friendId, UUID currentUserId) {
        CloseFriend closeFriend = closeFriendRepository.findByUserIdAndFriendId(currentUserId, friendId)
                .orElseThrow(() -> new ResourceNotFoundException("User is not in your close friends list"));
        closeFriendRepository.delete(closeFriend);
    }

    /**
     * get (pageable)
     */
    @Transactional(readOnly = true)
    public Page<CloseFriendResponse> getCloseFriends(UUID currentUserId, Pageable pageable) {
        Page<CloseFriend> page = closeFriendRepository.findAllByUserId(currentUserId, pageable);
        if (page.isEmpty()) {
            return page.map(cf -> null);
        }
        // Gom danh sách friend IDs để query profile 1 lần duy nhất
        List<UUID> friendIds = page.stream()
                .map(cf -> cf.getFriend().getId())
                .toList();
        Map<UUID, UserProfile> profileMap = userProfileRepository.findAllById(friendIds)
                .stream()
                .collect(Collectors.toMap(UserProfile::getUserId, Function.identity()));
        return page.map(cf -> {
            UserProfile profile = profileMap.get(cf.getFriend().getId());
            return CloseFriendResponse.of(cf, profile);
        });
    }

    @Transactional(readOnly = true)
    public boolean isCloseFriend(UUID userId, UUID friendId) {
        return closeFriendRepository.existsByUserIdAndFriendId(userId, friendId);
    }


    /**
     * Xóa sạch quan hệ bạn thân giữa 2 người (2 chiều).
     * Được gọi tự động từ FriendRequestService khi hủy kết bạn (unfriend) hoặc chặn (block).
     */
    @Transactional
    public void removeAllBetween(UUID userA, UUID userB) {
        closeFriendRepository.deleteAllBetweenUsers(userA, userB);
    }

}
