package com.ryan.socialplatform.comment;

import com.ryan.socialplatform.comment.dto.*;
import com.ryan.socialplatform.comment.enums.CommentSortBy;
import com.ryan.socialplatform.comment.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * GET posts/{postId}/comments?sortBy=POPULAR&page=0&size=10
     */
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<Slice<CommentResponse>> getPostComments(
            @PathVariable UUID postId,
            @RequestParam(defaultValue = "POPULAR") CommentSortBy sortBy,
            @PageableDefault(size = 10) Pageable pageable,
            @AuthenticationPrincipal UUID currentUserId
    ) {
        Slice<CommentResponse> comments = commentService.getPostComments(currentUserId, postId, sortBy, pageable);
        return ResponseEntity.ok(comments);
    }

    /**
     * Tạo bình luận
     * POST /posts/{postId}/comments
     */
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable UUID postId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal UUID currentUserId
    ) {
        CommentResponse response = commentService.create(currentUserId, postId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Trả lời một bình luận (Reply)
     * POST /comments/{commentId}/replies
     */
    @PostMapping("/comments/{commentId}/replies")
    public ResponseEntity<ReplyResponse> createReply(
            @PathVariable UUID commentId,
            @Valid @RequestBody CreateReplyRequest request,
            @AuthenticationPrincipal UUID currentUserId
    ) {
        ReplyResponse response = commentService.createReply(currentUserId, commentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    /**
     * Lấy danh sách câu trả lời của bình luận (Replies)
     * GET /comments/{commentId}/replies?page=0&size=10
     */
    @GetMapping("/comments/{commentId}/replies")
    public ResponseEntity<Slice<ReplyResponse>> getCommentReplies(
            @PathVariable UUID commentId,
            @PageableDefault(size = 10) Pageable pageable,
            @AuthenticationPrincipal UUID currentUserId
    ) {
        Slice<ReplyResponse> replies = commentService.getCommentReplies(currentUserId, commentId, pageable);
        return ResponseEntity.ok(replies);
    }

    /**
     * Chỉnh sửa bình luận
     * PUT /comments/{commentId}
     */
    @PutMapping({"/comments/{commentId}"})
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable UUID commentId,
            @Valid @RequestBody UpdateCommentRequest request,
            @AuthenticationPrincipal UUID currentUserId
    ) {
        CommentResponse response = commentService.updateComment(currentUserId, commentId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Xóa bình luận
     * DELETE /comments/{commentId}
     */
    @DeleteMapping({"/comments/{commentId}"})
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID commentId,
            @AuthenticationPrincipal UUID currentUserId
    ) {
        commentService.deleteComment(currentUserId, commentId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Ghim bình luận
     * PUT /comments/{commentId}/pin
     */
    @PutMapping({"/comments/{commentId}/pin"})
    public ResponseEntity<CommentResponse> pinComment(
            @PathVariable UUID commentId,
            @AuthenticationPrincipal UUID currentUserId
    ) {
        CommentResponse response = commentService.pinComment(currentUserId, commentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Bỏ ghim bình luận
     * PUT /comments/{commentId}/unpin
     */
    @PutMapping({"/comments/{commentId}/unpin"})
    public ResponseEntity<CommentResponse> unpinComment(
            @PathVariable UUID commentId,
            @AuthenticationPrincipal UUID currentUserId
    ) {
        CommentResponse response = commentService.unpinComment(currentUserId, commentId);
        return ResponseEntity.ok(response);
    }

    /**
     * getCommentById
     * GET /comments/{commentId}
     */
    @GetMapping({"/comments/{commentId}"})
    public ResponseEntity<CommentResponse> getComment(
            @PathVariable UUID commentId,
            @AuthenticationPrincipal UUID currentUserId
    ) {
        CommentResponse response = commentService.getCommentById(currentUserId, commentId);
        return ResponseEntity.ok(response);

    }

}
