package com.ryan.socialplatform.user.repository;

import com.ryan.socialplatform.user.entity.UserAppRole;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface UserAppRoleRepository extends JpaRepository<UserAppRole, UUID> {
    @Query("SELECT r FROM UserAppRole r WHERE r.user.id = :userId")
    List<UserAppRole> findAllByUserId(@Param("userId") UUID userId);
}
