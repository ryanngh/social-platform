package com.ryan.socialplatform.post;

import com.ryan.socialplatform.post.dto.CreatePostRequest;
import com.ryan.socialplatform.post.dto.CreatePostResponse;
import com.ryan.socialplatform.post.dto.PostResponse;
import com.ryan.socialplatform.post.dto.UpdatePostRequest;
import com.ryan.socialplatform.post.dto.UpdatePostResponse;
import com.ryan.socialplatform.post.service.PostService;
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
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    /**
     * Tạo bài viết mới
     * POST /posts
     */
    @PostMapping("/posts")
    public ResponseEntity<CreatePostResponse> createPost(
            @Valid @RequestBody CreatePostRequest request,
            @AuthenticationPrincipal UUID currentUserId
    ) {
        CreatePostResponse response = postService.create(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lấy chi tiết bài viết theo ID
     * GET /posts/{postId}
     */
    @GetMapping("/posts/{postId}")
    public ResponseEntity<PostResponse> getPost(
            @PathVariable UUID postId,
            @AuthenticationPrincipal UUID currentUserId
    ) {
        PostResponse response = postService.getPost(currentUserId, postId);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy danh sách bài viết trên trang cá nhân của một người dùng
     * GET /users/{userId}/posts
     * GET /posts/user/{userId}
     */
    @GetMapping({"/users/{userId}/posts", "/posts/user/{userId}"})
    public ResponseEntity<Slice<PostResponse>> getUserPosts(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UUID currentUserId,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Slice<PostResponse> posts = postService.getUserPost(userId, currentUserId, pageable);
        return ResponseEntity.ok(posts);
    }

    /**
     * Lấy danh sách bài viết của chính người dùng hiện tại
     * GET /users/me/posts
     * GET /posts/me
     */
    @GetMapping({"/users/me/posts", "/posts/me"})
    public ResponseEntity<Slice<PostResponse>> getMyPosts(
            @AuthenticationPrincipal UUID currentUserId,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Slice<PostResponse> posts = postService.getUserPost(currentUserId, currentUserId, pageable);
        return ResponseEntity.ok(posts);
    }

    /**
     * Chỉnh sửa bài viết
     * PUT /posts/{postId}
     */
    @PutMapping("/posts/{postId}")
    public ResponseEntity<UpdatePostResponse> updatePost(
            @PathVariable UUID postId,
            @Valid @RequestBody UpdatePostRequest request,
            @AuthenticationPrincipal UUID currentUserId
    ) {
        UpdatePostResponse response = postService.updatePost(currentUserId, postId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Xóa mềm bài viết
     * DELETE /posts/{postId}
     */
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable UUID postId,
            @AuthenticationPrincipal UUID currentUserId
    ) {
        postService.deletePost(currentUserId, postId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Khôi phục bài viết đã xóa mềm
     * POST /posts/{postId}/restore
     */
    @PostMapping("/posts/{postId}/restore")
    public ResponseEntity<Void> restorePost(
            @PathVariable UUID postId,
            @AuthenticationPrincipal UUID currentUserId
    ) {
        postService.restorePost(currentUserId, postId);
        return ResponseEntity.ok().build();
    }
}
