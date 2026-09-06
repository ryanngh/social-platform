package com.ryan.socialplatform.post.repository;

import com.ryan.socialplatform.post.entity.HashTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface HashTagRepository extends JpaRepository<HashTag, UUID> {
    Optional<HashTag> findByTag(String tag);
}
