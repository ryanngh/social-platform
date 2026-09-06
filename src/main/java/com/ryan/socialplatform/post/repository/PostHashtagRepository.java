package com.ryan.socialplatform.post.repository;

import com.ryan.socialplatform.post.entity.PostHashTag;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface PostHashtagRepository extends JpaRepository<PostHashTag, UUID> {
    @Query("SELECT ph.hashtag.tag FROM PostHashTag ph WHERE ph.post.id = :postId")
    List<String> findHashtagNamesByPostId(@Param("postId") UUID postId);
}
