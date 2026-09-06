package com.ryan.socialplatform.post.repository;

import com.ryan.socialplatform.post.entity.PostMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PostMediaRepository extends JpaRepository<PostMedia, UUID> {
    // SELECT * FROM post_media WHERE post_id = ? ORDER BY display_order ASC
    List<PostMedia> findAllByPostIdOrderByDisplayOrderAsc(UUID postId);
}
