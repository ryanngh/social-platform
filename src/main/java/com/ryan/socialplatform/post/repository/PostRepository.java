package com.ryan.socialplatform.post.repository;

import com.ryan.socialplatform.post.entity.Post;
import com.ryan.socialplatform.post.enums.PostVisibility;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {

    Optional<Post> findByIdAndDeletedAtIsNull(UUID id);

    @Query("""
                SELECT p FROM Post p
                WHERE p.author.id = :authorId
                  AND p.deletedAt IS NULL
                  AND p.visibility IN :visibilities
            """)
    Slice<Post> findProfilePosts(
            @Param("authorId") UUID authorId,
            @Param("visibilities") Collection<PostVisibility> visibilities,
            Pageable pageable
    );

    @Query("""
                SELECT p FROM Post p
                WHERE p.author.id = :authorId
                  AND p.deletedAt IS NULL
            """)
    Slice<Post> findOwnProfilePosts(
            @Param("authorId") UUID authorId,
            Pageable pageable
    );
}
