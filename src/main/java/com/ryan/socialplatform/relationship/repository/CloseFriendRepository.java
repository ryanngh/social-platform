package com.ryan.socialplatform.relationship.repository;

import com.ryan.socialplatform.relationship.entity.CloseFriend;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CloseFriendRepository extends JpaRepository<CloseFriend, UUID> {

    // Kiểm tra xem friendId có trong danh sách bạn thân của userId không
    boolean existsByUserIdAndFriendId(UUID userId, UUID friendId);

    // Tìm bản ghi để xóa khỏi danh sách
    Optional<CloseFriend> findByUserIdAndFriendId(UUID userId, UUID friendId);

    // Lấy danh sách bạn thân của user (có phân trang)
    Page<CloseFriend> findAllByUserId(UUID userId, Pageable pageable);

    // Xóa liên kết bạn thân 2 chiều khi unfriend hoặc block
    @Modifying
    @Query("""
        DELETE FROM CloseFriend cf
        WHERE (cf.user.id = :userA AND cf.friend.id = :userB)
           OR (cf.user.id = :userB AND cf.friend.id = :userA)
    """)
    void deleteAllBetweenUsers(@Param("userA") UUID userA, @Param("userB") UUID userB);
}