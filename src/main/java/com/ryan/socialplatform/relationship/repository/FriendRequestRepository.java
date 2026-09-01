package com.ryan.socialplatform.relationship.repository;

import com.ryan.socialplatform.relationship.entity.FriendRequest;
import com.ryan.socialplatform.relationship.enums.FriendRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, UUID> {

    // 1 query duy nhất: Lấy bản ghi quan hệ giữa 2 người (bất kể chiều gửi A->B hay B->A)
    @Query("""
        SELECT fr FROM FriendRequest fr
        WHERE (fr.sender.id = :userA AND fr.receiver.id = :userB)
           OR (fr.sender.id = :userB AND fr.receiver.id = :userA)
    """)
    Optional<FriendRequest> findBetweenUsers(@Param("userA") UUID userA, @Param("userB") UUID userB);

    // Lấy danh sách lời mời nhận được theo trạng thái
    Page<FriendRequest> findByReceiverIdAndStatus(UUID receiverId, FriendRequestStatus status, Pageable pageable);

    // Lấy danh sách lời mời đã gửi đi theo trạng thái
    Page<FriendRequest> findBySenderIdAndStatus(UUID senderId, FriendRequestStatus status, Pageable pageable);
}
