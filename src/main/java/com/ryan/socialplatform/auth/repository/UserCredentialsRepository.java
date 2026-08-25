package com.ryan.socialplatform.auth.repository;

import com.ryan.socialplatform.user.entity.UserCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserCredentialsRepository extends JpaRepository<UserCredentials, UUID> {
    @Query("""
                SELECT c FROM UserCredentials c
                WHERE c.email = :identifier OR c.phoneNumber = :identifier
            """)
    Optional<UserCredentials> findByIdentifier(@Param("identifier") String identifier);
}