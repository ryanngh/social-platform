package com.ryan.socialplatform.comment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ryan.socialplatform.comment.entity.Comment;

import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
}
