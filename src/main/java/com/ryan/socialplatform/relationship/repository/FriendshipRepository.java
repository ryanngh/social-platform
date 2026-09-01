package com.ryan.socialplatform.relationship.repository;

import com.ryan.socialplatform.relationship.entity.Friendship;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    // Kiểm tra xem 2 người đã là bạn bè chưa
    @Query("""
        SELECT COUNT(f) > 0
        FROM Friendship f
        WHERE (f.user1.id = :userA AND f.user2.id = :userB)
           OR (f.user1.id = :userB AND f.user2.id = :userA)
    """)
    boolean areFriends(@Param("userA") UUID userA, @Param("userB") UUID userB);

    // Lấy bản ghi friendship giữa 2 người (để Unfriend)
    @Query("""
        SELECT f FROM Friendship f
        WHERE (f.user1.id = :userA AND f.user2.id = :userB)
           OR (f.user1.id = :userB AND f.user2.id = :userA)
    """)
    Optional<Friendship> findBetweenUsers(@Param("userA") UUID userA, @Param("userB") UUID userB);

    // Lấy danh sách bạn bè của 1 user (phân trang)
    @Query("""
        SELECT f FROM Friendship f
        WHERE f.user1.id = :userId OR f.user2.id = :userId
    """)
    Page<Friendship> findAllByUserId(@Param("userId") UUID userId, Pageable pageable);

    // Lấy IDs của bạn bè (để tính bạn chung)
    @Query("""
        SELECT CASE
            WHEN f.user1.id = :userId THEN f.user2.id
            ELSE f.user1.id
        END
        FROM Friendship f
        WHERE f.user1.id = :userId OR f.user2.id = :userId
    """)
    java.util.List<UUID> findFriendIds(@Param("userId") UUID userId);

    // Bạn chung giữa 2 người
    @Query("""
        SELECT f FROM Friendship f
        WHERE (f.user1.id = :userId OR f.user2.id = :userId)
          AND (
            (f.user1.id IN (
                SELECT CASE WHEN f2.user1.id = :targetId THEN f2.user2.id ELSE f2.user1.id END
                FROM Friendship f2
                WHERE f2.user1.id = :targetId OR f2.user2.id = :targetId
            ))
            OR
            (f.user2.id IN (
                SELECT CASE WHEN f2.user1.id = :targetId THEN f2.user2.id ELSE f2.user1.id END
                FROM Friendship f2
                WHERE f2.user1.id = :targetId OR f2.user2.id = :targetId
            ))
          )
    """)
    Page<Friendship> findMutualFriends(
            @Param("userId") UUID userId,
            @Param("targetId") UUID targetId,
            Pageable pageable
    );
}
