package com.ryan.socialplatform.comment.repository;

import com.ryan.socialplatform.comment.entity.CommentMention;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CommentMentionRepository extends JpaRepository<CommentMention, UUID> {

    @Query("""
        SELECT cm FROM CommentMention cm
        WHERE cm.comment.id IN :commentIds
        ORDER BY cm.createdAt ASC
    """)
    List<CommentMention> findByCommentIdIn(@Param("commentIds") Collection<UUID> commentIds);
}
