package com.ryan.socialplatform.relationship.repository;

import com.ryan.socialplatform.relationship.entity.UserBlock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserBlockRepository extends JpaRepository<UserBlock, UUID> {

    // Kiểm tra xem có block ở bất kỳ chiều nào giữa 2 người không
    @Query("""
        SELECT COUNT(ub) > 0
        FROM UserBlock ub
        WHERE (ub.blocker.id = :userA AND ub.blocked.id = :userB)
           OR (ub.blocker.id = :userB AND ub.blocked.id = :userA)
    """)
    boolean existsBlockBetween(@Param("userA") UUID userA, @Param("userB") UUID userB);

    // Lấy bản ghi block (để Unblock)
    Optional<UserBlock> findByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

    // Kiểm tra chính xác chiều mình block người kia (dùng trong Unblock)
    boolean existsByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

    // Danh sách những người mình đã chặn (phân trang)
    Page<UserBlock> findAllByBlockerId(UUID blockerId, Pageable pageable);
}
