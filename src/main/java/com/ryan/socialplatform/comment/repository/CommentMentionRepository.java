package com.ryan.socialplatform.comment.repository;

import com.ryan.socialplatform.comment.entity.CommentMention;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CommentMentionRepository extends JpaRepository<CommentMention, UUID> {
}
