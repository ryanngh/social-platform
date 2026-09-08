package com.ryan.socialplatform.comment.repository;

import com.ryan.socialplatform.comment.entity.CommentMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CommentMediaRepository extends JpaRepository<CommentMedia, UUID> {
}
