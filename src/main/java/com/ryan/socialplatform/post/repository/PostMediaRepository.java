package com.ryan.socialplatform.post.repository;

import com.ryan.socialplatform.post.entity.PostMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PostMediaRepository extends JpaRepository<PostMedia, UUID> {
    // SELECT * FROM post_media WHERE post_id = ? ORDER BY display_order ASC
    List<PostMedia> findAllByPostIdOrderByDisplayOrderAsc(UUID postId);

    // Batch query: lấy media của nhiều bài viết cùng lúc
    List<PostMedia> findAllByPostIdInOrderByDisplayOrderAsc(Collection<UUID> postIds);

    boolean existsByPostId(UUID postId);

    @Modifying
    @Query("DELETE FROM PostMedia pm WHERE pm.post.id = :postId")
    void deleteByPostId(@Param("postId") UUID postId);
}

