package com.ryan.socialplatform.post.repository;

import com.ryan.socialplatform.post.entity.PostTag;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PostTagRepository extends JpaRepository<PostTag, UUID> {
    @Query("SELECT pt.taggedUser.id FROM PostTag pt WHERE pt.post.id = :postId")
    List<UUID> findTaggedUserIdsByPostId(@Param("postId") UUID postId);

    // Batch query: lấy toàn bộ PostTag kèm User theo danh sách postIds (JOIN FETCH để tránh N+1)
    @Query("SELECT pt FROM PostTag pt JOIN FETCH pt.taggedUser WHERE pt.post.id IN :postIds")
    List<PostTag> findAllByPostIdInWithTaggedUser(@Param("postIds") Collection<UUID> postIds);
}
