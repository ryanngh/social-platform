package com.ryan.socialplatform.post.repository;

import com.ryan.socialplatform.post.entity.PostHashTag;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PostHashtagRepository extends JpaRepository<PostHashTag, UUID> {
    @Query("SELECT ph.hashtag.tag FROM PostHashTag ph WHERE ph.post.id = :postId")
    List<String> findHashtagNamesByPostId(@Param("postId") UUID postId);

    // Batch query: lấy toàn bộ PostHashTag kèm HashTag theo danh sách postIds (JOIN FETCH để tránh N+1)
    @Query("SELECT ph FROM PostHashTag ph JOIN FETCH ph.hashtag WHERE ph.post.id IN :postIds")
    List<PostHashTag> findAllByPostIdInWithHashtag(@Param("postIds") Collection<UUID> postIds);

    @Modifying
    @Query("DELETE FROM PostHashTag ph WHERE ph.post.id = :postId")
    void deleteByPostId(@Param("postId") UUID postId);
}

