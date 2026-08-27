// user/repository/UserRepository.java
package com.ryan.socialplatform.user.repository;

import com.ryan.socialplatform.user.dto.UserResponse;
import com.ryan.socialplatform.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("""
                SELECT new com.ryan.socialplatform.user.dto.UserResponse(
                    u.id, p.firstName, p.lastName,
                    TRIM(CONCAT(COALESCE(p.firstName, ''), ' ', COALESCE(p.lastName, ''))),
                    p.avatarUrl, p.bannerUrl, p.bio,
                    p.pronouns, p.location, p.websiteUrl, p.birthday, p.pronunciation,
                    u.status, u.verified,
                    (CASE WHEN :viewerId IS NOT NULL AND u.id = :viewerId THEN true ELSE false END),
                    u.createdAt, u.updatedAt
                )
                FROM User u
                LEFT JOIN UserProfile p ON p.userId = u.id
                WHERE u.id = :userId
            """)
    Optional<UserResponse> findProfileByIdAndViewer(@Param("userId") UUID userId, @Param("viewerId") UUID viewerId);

    default Optional<UserResponse> findProfileById(UUID userId) {
        return findProfileByIdAndViewer(userId, null);
    }
                                                                    
}