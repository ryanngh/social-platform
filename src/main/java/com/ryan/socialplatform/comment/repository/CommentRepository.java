package com.ryan.socialplatform.comment.repository;

import com.ryan.socialplatform.comment.enums.CommentSortBy;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ryan.socialplatform.comment.entity.Comment;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    Optional<Comment> findByIdAndDeletedAtIsNull(UUID commentId);

    @Query("""
                SELECT c FROM Comment c
                WHERE c.post.id = :postId
                  AND c.pinned = true
                  AND c.parentComment IS NULL
                  AND c.deletedAt IS NULL
            """)
    Optional<Comment> findPinnedComment(@Param("postId") UUID postId);

    @Query("""
            SELECT c
            FROM Comment c
            WHERE c.post.id = :postId
                AND c.parentComment IS NULL
                AND c.deletedAt IS NULL
            """)
    Slice<Comment> findTopLevelComments(@Param("postId") UUID postId,
                                        Pageable pageable);

    @Query("""
            SELECT c FROM Comment c
            WHERE c.post.id = :postId
              AND c.parentComment IS NULL
              AND c.deletedAt IS NULL
              AND c.id != :excludedCommentId""")
    Slice<Comment> findTopLevelCommentsExcluding(
            @Param("postId") UUID postId,
            @Param("excludedCommentId") UUID excludedCommentId,
            Pageable pageable
    );

    @Query("""
                SELECT c
                FROM Comment c
                WHERE c.parentComment.id = :commentId
                  AND c.deletedAt IS NULL
                ORDER BY c.createdAt ASC, c.id ASC
            """)
    Slice<Comment> findReplies(
            @Param("commentId") UUID commentId,
            Pageable pageable
    );

    @Modifying(flushAutomatically = true)
    @Query("""
    UPDATE Comment c
    SET c.pinned = false, c.pinnedAt = null
    WHERE c.post.id = :postId AND c.pinned = true
""")
    int unpinAllByPostId(@Param("postId") UUID postId);

    @Modifying
    @Query("UPDATE Comment c SET c.replyCount = c.replyCount + 1 WHERE c.id = :id")
    int incrementReplyCount(@Param("id") UUID id);

    @Modifying
    @Query("""
    UPDATE Comment c 
    SET c.replyCount = CASE WHEN c.replyCount > 0 THEN c.replyCount - 1 ELSE 0 END 
    WHERE c.id = :id
""")
    int decrementReplyCount(@Param("id") UUID id);
}
