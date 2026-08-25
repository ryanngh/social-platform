package com.ryan.socialplatform.user.repository;

import com.ryan.socialplatform.user.entity.UserCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserCredentialsRepository extends JpaRepository<UserCredentials, UUID> {

    Optional<UserCredentials> findByEmailAndDeletedFalse(String email);

    Optional<UserCredentials> findByPhoneNumberAndDeletedFalse(String phoneNumber);

    Optional<UserCredentials> findByUserIdAndDeletedFalse(UUID userId);

    Optional<UserCredentials> findByEmail(String email);

    Optional<UserCredentials> findByPhoneNumber(String phoneNumber);
}
