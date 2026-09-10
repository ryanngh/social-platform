package com.ryan.socialplatform.comment.repository;

import com.ryan.socialplatform.comment.entity.CommentMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CommentMediaRepository extends JpaRepository<CommentMedia, UUID> {
    @Query("""
        SELECT m FROM CommentMedia m
        WHERE m.comment.id IN :commentIds
        ORDER BY m.displayOrder ASC
    """)
    List<CommentMedia> findByCommentIdInOrderByDisplayOrderAsc(@Param("commentIds") Collection<UUID> commentIds);}
