package com.ryan.socialplatform.post.repository;

import com.ryan.socialplatform.post.entity.PostTag;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface PostTagRepository extends JpaRepository<PostTag, UUID> {
    @Query("SELECT pt.taggedUser.id FROM PostTag pt WHERE pt.post.id = :postId")
    List<UUID> findTaggedUserIdsByPostId(@Param("postId") UUID postId);
}
