package com.ryan.socialplatform.comment.service;

import com.ryan.socialplatform.comment.dto.*;
import com.ryan.socialplatform.comment.entity.Comment;
import com.ryan.socialplatform.comment.entity.CommentMedia;
import com.ryan.socialplatform.comment.entity.CommentMention;
import com.ryan.socialplatform.comment.enums.CommentSortBy;
import com.ryan.socialplatform.comment.repository.CommentMediaRepository;
import com.ryan.socialplatform.comment.repository.CommentMentionRepository;
import com.ryan.socialplatform.comment.repository.CommentRepository;
import com.ryan.socialplatform.relationship.repository.FriendshipRepository;
import com.ryan.socialplatform.relationship.repository.UserBlockRepository;
import com.ryan.socialplatform.post.entity.Post;
import com.ryan.socialplatform.post.repository.PostRepository;
import com.ryan.socialplatform.user.dto.UserSummaryResponse;
import com.ryan.socialplatform.user.entity.User;
import com.ryan.socialplatform.user.entity.UserProfile;
import com.ryan.socialplatform.user.exceptions.UserNotFoundException;
import com.ryan.socialplatform.user.repository.UserProfileRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private final CommentMediaRepository commentMediaRepository;
    private final CommentMentionRepository commentMentionRepository;
    private final PostRepository postRepository;
    private final UserBlockRepository userBlockRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserProfileRepository userProfileRepository;

    public CommentService(CommentRepository commentRepository, CommentMediaRepository commentMediaRepository, CommentMentionRepository commentMentionRepository, PostRepository postRepository, UserBlockRepository userBlockRepository, FriendshipRepository friendshipRepository, UserProfileRepository userProfileRepository) {
        this.commentRepository = commentRepository;
        this.commentMediaRepository = commentMediaRepository;
        this.commentMentionRepository = commentMentionRepository;
        this.postRepository = postRepository;
        this.userBlockRepository = userBlockRepository;
        this.friendshipRepository = friendshipRepository;
        this.userProfileRepository = userProfileRepository;
    }

    /**
     * POST Comment
     */
    @Transactional
    public CommentResponse create(UUID currentUserId, UUID postId, CreateCommentRequest request) {
        // 1. Validate input
        boolean hasContent = request.content() != null && !request.content().isBlank();

        boolean hasMedia = request.media() != null && !request.media().isEmpty();

        if (!hasContent && !hasMedia) {
            throw new IllegalArgumentException("Comment must contain content or media");
            // TODO: CommentContentRequiredException
        }

        // 2. Visibility & Block
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId).orElseThrow(() -> new IllegalArgumentException("Post with id " + postId + " does not exist"));
        // TODO: PostNotFoundException

        UUID authorId = post.getAuthor().getId();

        // Không cho comment nếu đang block nhau
        if (!currentUserId.equals(authorId) && userBlockRepository.existsBlockBetween(currentUserId, authorId)) {
            throw new IllegalArgumentException("You cannot comment on this post");
            // TODO: UserBlockedException
        }

        switch (post.getVisibility()) {
            case PUBLIC -> {
                // Anyone can comment
            }

            case FRIENDS -> {
                if (!currentUserId.equals(authorId) && !friendshipRepository.areFriends(currentUserId, authorId)) {
                    throw new IllegalArgumentException("You must be friends with the post author to comment");
                }
            }

            case CLOSE_FRIENDS -> {
                // TODO: Implement Close Friends module
            }

            case PRIVATE -> {
                if (!currentUserId.equals(authorId)) {
                    throw new IllegalArgumentException("You cannot comment on this post");
                }
            }
        }

        // 3. Validate mentions
        List<UserProfile> taggedProfiles = validateAndGetTaggedProfiles(currentUserId, request.mentionedUserIds());

        // 4. Get current user's profile
        UserProfile authorProfile = userProfileRepository.findById(currentUserId).orElseThrow(() -> new UserNotFoundException(currentUserId));

        User author = authorProfile.getUser();

        // 5. Create and save comment
        Comment comment = new Comment(post, author, null, request.content());

        comment = commentRepository.save(comment);

        // 6. Save comment media
        List<CommentMediaResponse> mediaResponses = List.of();

        if (hasMedia) {
            List<CommentMedia> mediaList = getCommentMedia(request.media(), comment);

            mediaResponses = commentMediaRepository.saveAll(mediaList).stream().map(CommentMediaResponse::from).toList();
        }

        // 7. Save mentions
        List<UserSummaryResponse> mentionResponses = List.of();

        if (!taggedProfiles.isEmpty()) {
            Comment finalComment = comment;
            List<CommentMention> commentMentions = taggedProfiles.stream().map(profile -> new CommentMention(finalComment, profile.getUser())).toList();

            commentMentionRepository.saveAll(commentMentions);

            mentionResponses = taggedProfiles.stream().map(UserSummaryResponse::from).toList();
        }

        // 8. Return response
        CommentPermissionsResponse permissions = CommentPermissionsResponse.of(
                true, // canEdit (vừa tạo xong thì là chính chủ)
                true, // canDelete (chính chủ luôn xóa được)
                currentUserId.equals(authorId) // canPin (nếu kiêm luôn chủ post thì được pin)
        );

        return CommentResponse.of(
                comment.getId(), post.getId(), null,
                UserSummaryResponse.from(authorProfile),
                comment.getContent(), mediaResponses, mentionResponses,
                comment.getLikeCount(), comment.getReplyCount(),
                comment.isPinned(), comment.getPinnedAt(),
                comment.getCreatedAt(), comment.getEditedAt(), comment.getUpdatedAt(),
                permissions
        );
    }

    /**
     * Reply Comment
     **/
    @Transactional
    public ReplyResponse createReply(UUID currentUserId, UUID commentId, CreateReplyRequest request) {
        // 1. Validate input
        boolean hasContent = request.content() != null && !request.content().isBlank();
        boolean hasMedia = request.media() != null && !request.media().isEmpty();

        if (!hasContent && !hasMedia) {
            throw new IllegalArgumentException("Comment must contain content or media");
            // TODO: CommentContentRequiredException
        }

        // 2. Find target comment
        Comment targetComment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment with id " + commentId + " does not exist"));

        // 3. Find Post & Check Visibility / Block
        Post post = postRepository.findByIdAndDeletedAtIsNull(targetComment.getPost().getId())
                .orElseThrow(() -> new IllegalArgumentException("Post does not exist or has been deleted"));

        UUID postAuthorId = post.getAuthor().getId();

        // Không cho comment nếu đang block nhau với chủ post
        if (!currentUserId.equals(postAuthorId) && userBlockRepository.existsBlockBetween(currentUserId, postAuthorId)) {
            throw new IllegalArgumentException("You cannot comment on this post");
        }

        switch (post.getVisibility()) {
            case PUBLIC -> {
            }
            case FRIENDS -> {
                if (!currentUserId.equals(postAuthorId) && !friendshipRepository.areFriends(currentUserId, postAuthorId)) {
                    throw new IllegalArgumentException("You must be friends with the post author to comment");
                }
            }
            case CLOSE_FRIENDS -> {
                // TODO: Implement Close Friends module
            }
            case PRIVATE -> {
                if (!currentUserId.equals(postAuthorId)) {
                    throw new IllegalArgumentException("You cannot comment on this post");
                }
            }
        }

        // 4. Chuẩn hóa 2 tầng (Facebook Flattened Model)
        Comment actualParent;
        User replyToUser = targetComment.getAuthor();
        List<UUID> mentionIds = new ArrayList<>(request.mentionedUserIds());

        if (targetComment.isTopLevel()) {
            // Trường hợp A: Trả lời trực tiếp comment cấp 1
            actualParent = targetComment;
        } else {
            // Trường hợp B: Trả lời một reply con -> quy về comment gốc cấp 1
            actualParent = targetComment.getParentComment();

            // Kiểm tra comment gốc có bị xóa hay chưa
            if (actualParent == null || actualParent.isDeleted()) {
                throw new IllegalArgumentException("Parent comment has been deleted");
            }

            // Tự động tag tác giả của reply con (nếu có author và không tự tag chính mình)
            if (replyToUser != null && !replyToUser.getId().equals(currentUserId)) {
                if (!mentionIds.contains(replyToUser.getId())) {
                    mentionIds.add(replyToUser.getId());
                }
            }
        }

        // 5. Validate toàn bộ danh sách mentions (cả tag tay lẫn auto-tag)
        List<UserProfile> taggedProfiles = validateAndGetTaggedProfiles(currentUserId, mentionIds);

        // 6. Lấy profile tác giả viết reply (current user) và profile người được reply (replyToUser)
        UserProfile authorProfile = userProfileRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException(currentUserId));
        User author = authorProfile.getUser();

        UserProfile replyToProfile = null;
        if (replyToUser != null) {
            if (replyToUser.getId().equals(currentUserId)) {
                replyToProfile = authorProfile;
            } else {
                replyToProfile = userProfileRepository.findById(replyToUser.getId()).orElse(null);
            }
        }

        // 7. Tạo đối tượng Comment và lưu vào Database
        Comment replyComment = new Comment(post, author, actualParent, request.content());
        replyComment = commentRepository.save(replyComment);

        // 8. Lưu comment media
        List<CommentMediaResponse> mediaResponses = List.of();
        if (hasMedia) {
            List<CommentMedia> mediaList = getCommentMedia(request.media(), replyComment);
            mediaResponses = commentMediaRepository.saveAll(mediaList).stream()
                    .map(CommentMediaResponse::from)
                    .toList();
        }

        // 9. Lưu mentions
        List<UserSummaryResponse> mentionResponses = List.of();
        if (!taggedProfiles.isEmpty()) {
            Comment finalReplyComment = replyComment;
            List<CommentMention> commentMentions = taggedProfiles.stream()
                    .map(profile -> new CommentMention(finalReplyComment, profile.getUser()))
                    .toList();

            commentMentionRepository.saveAll(commentMentions);

            mentionResponses = taggedProfiles.stream()
                    .map(UserSummaryResponse::from)
                    .toList();
        }

        // 10. Cập nhật bộ đếm câu trả lời nguyên tử (Atomic Update)
        commentRepository.incrementReplyCount(actualParent.getId());

        // TODO: (Tùy chọn) Bắn thông báo:
        // - Gửi thông báo cho tác giả được trả lời trực tiếp (replyToUser)
        // - Gửi thông báo cho chủ comment gốc (actualParent.getAuthor(), nếu khác replyToUser)

        // 11. Trả về ReplyResponse
        return ReplyResponse.of(
                replyComment.getId(),
                post.getId(),
                actualParent.getId(),
                UserSummaryResponse.from(authorProfile),
                UserSummaryResponse.from(replyToProfile),
                replyComment.getContent(),
                mediaResponses,
                mentionResponses,
                replyComment.getLikeCount(),
                replyComment.getCreatedAt(),
                replyComment.getEditedAt(),
                replyComment.getUpdatedAt()
        );
    }

    /**
     * getPostComments
     */

    public Slice<CommentResponse> getPostComments(UUID currentUserId, UUID postId, CommentSortBy sortBy, Pageable pageable) {
        // 1. PERMISSION & VISIBILITY
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post does not exist or has been deleted"));
        UUID postAuthorId = post.getAuthor().getId();

        if (!currentUserId.equals(postAuthorId) && userBlockRepository.existsBlockBetween(currentUserId, postAuthorId)) {
            throw new IllegalArgumentException("You cannot comment on this post");
        }

        switch (post.getVisibility()) {
            case PUBLIC -> {
            }
            case FRIENDS -> {
                if (!currentUserId.equals(postAuthorId) && !friendshipRepository.areFriends(currentUserId, postAuthorId)) {
                    throw new IllegalArgumentException("You must be friends with the post author to comment");
                }
            }
            case CLOSE_FRIENDS -> {
                // TODO: Implement Close Friends module
            }
            case PRIVATE -> {
                if (!currentUserId.equals(postAuthorId)) {
                    throw new IllegalArgumentException("You cannot comment on this post");
                }
            }
        }

        // PINNED COMMENT

        Comment pinnedComment = null;
        UUID pinnedCommentId = null;

        if (pageable.getPageNumber() == 0) {
            pinnedComment = commentRepository
                    .findPinnedComment(postId)
                    .orElse(null);

            if (pinnedComment != null) {
                pinnedCommentId = pinnedComment.getId();
            }
        }

        // == SORT MODE ==

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                resolveSort(sortBy)
        );
        Slice<Comment> commentsSlice;
        if (pinnedCommentId != null) {
            commentsSlice = commentRepository.findTopLevelCommentsExcluding(
                    postId,
                    pinnedCommentId,
                    sortedPageable
            );
        } else {
            commentsSlice = commentRepository.findTopLevelComments(
                    postId,
                    sortedPageable
            );

        }

        // == BATCH QUERY ==
        // 1. gom pinnedcomment
        List<Comment> allComments = new ArrayList<>();
        if (pinnedCommentId != null) {
            allComments.add(pinnedComment);
        }

        allComments.addAll(commentsSlice.getContent());

        if (allComments.isEmpty()) {
            return new SliceImpl<>(List.of(), pageable, false);
        }

        List<UUID> commentIds = allComments.stream()
                .map(Comment::getId)
                .toList();
        Set<UUID> authorIds = allComments.stream()
                .map(c -> c.getAuthor() != null ? c.getAuthor().getId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 2. Media (1 Query)
        List<CommentMedia> mediaList = commentMediaRepository.findByCommentIdInOrderByDisplayOrderAsc(commentIds);

        Map<UUID, List<CommentMediaResponse>> mediaMap = mediaList.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getComment().getId(),
                        Collectors.mapping(CommentMediaResponse::from, Collectors.toList())
                ));

        // 3. Mentions (1 Query)
        List<CommentMention> mentionList = commentMentionRepository.findByCommentIdIn(commentIds);
        Set<UUID> allUserIdsToFetch = new HashSet<>(authorIds);
        mentionList.forEach(m -> {
            if (m.getMentionedUser() != null) {
                allUserIdsToFetch.add(m.getMentionedUser().getId());
            }
        });

        // 4.
        Map<UUID, UserProfile> userProfileMap = userProfileRepository.findAllById(allUserIdsToFetch)
                .stream()
                .collect(Collectors.toMap(UserProfile::getUserId, Function.identity()));
        Map<UUID, List<UserSummaryResponse>> mentionsMap = mentionList.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getComment().getId(),
                        Collectors.mapping(
                                m -> UserSummaryResponse.from(userProfileMap.get(m.getMentionedUser().getId())),
                                Collectors.toList()
                        )
                ));

        // PERMISSION AND RETURN

        List<CommentResponse> resultList = commentsSlice.getContent().stream()
                .map(comment -> mapToCommentResponse(
                        comment,
                        userProfileMap,
                        mediaMap,
                        mentionsMap,
                        currentUserId,
                        postAuthorId
                ))
                .collect(Collectors.toCollection(ArrayList::new));

        if (pinnedComment != null) {
            CommentResponse pinnedResponse = mapToCommentResponse(
                    pinnedComment,
                    userProfileMap,
                    mediaMap,
                    mentionsMap,
                    currentUserId,
                    postAuthorId
            );
            resultList.add(0, pinnedResponse);
        }

        return new SliceImpl<>(resultList, pageable, commentsSlice.hasNext());
    }

    private CommentResponse mapToCommentResponse(
            Comment comment,
            Map<UUID, UserProfile> userProfileMap,
            Map<UUID, List<CommentMediaResponse>> mediaMap,
            Map<UUID, List<UserSummaryResponse>> mentionsMap,
            UUID currentUserId,
            UUID postAuthorId
    ) {
        UUID authorId = comment.getAuthor() != null ? comment.getAuthor().getId() : null;
        UserProfile authorProfile = authorId != null ? userProfileMap.get(authorId) : null;

        List<CommentMediaResponse> media = mediaMap.getOrDefault(comment.getId(), List.of());
        List<UserSummaryResponse> mentions = mentionsMap.getOrDefault(comment.getId(), List.of());

        CommentPermissionsResponse permissions = resolvePermissions(comment, currentUserId, postAuthorId);

        return CommentResponse.of(
                comment.getId(),
                comment.getPost().getId(),
                comment.getParentComment() != null ? comment.getParentComment().getId() : null,
                UserSummaryResponse.from(authorProfile),
                comment.getContent(),
                media,
                mentions,
                comment.getLikeCount(),
                comment.getReplyCount(),
                comment.isPinned(),
                comment.getPinnedAt(),
                comment.getCreatedAt(),
                comment.getEditedAt(),
                comment.getUpdatedAt(),
                permissions
        );
    }

    private CommentPermissionsResponse resolvePermissions(Comment comment, UUID currentUserId, UUID postAuthorId) {
        if (currentUserId == null) {
            return CommentPermissionsResponse.of(false, false, false);
        }

        UUID commentAuthorId = comment.getAuthor() != null ? comment.getAuthor().getId() : null;

        boolean isCommentAuthor = currentUserId.equals(commentAuthorId);
        boolean isPostAuthor = currentUserId.equals(postAuthorId);

        boolean canEdit = isCommentAuthor;
        boolean canDelete = isCommentAuthor || isPostAuthor;
        boolean canPin = isPostAuthor && comment.isTopLevel();

        return CommentPermissionsResponse.of(canEdit, canDelete, canPin);
    }


/**
 * HELPER
 *
 */
private Sort resolveSort(CommentSortBy sortBy) {
    if (sortBy == null) {
        sortBy = CommentSortBy.POPULAR;
    }
    return switch (sortBy) {
        // Index: idx_comments_post_top_level_popular
        case POPULAR -> Sort.by(
                Sort.Order.desc("likeCount"),
                Sort.Order.desc("replyCount"),
                Sort.Order.asc("createdAt"),
                Sort.Order.asc("id")
        );
        // Index: idx_comments_post_top_level_created (chiều xuôi)
        case NEWEST -> Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id")
        );
        // Index: idx_comments_post_top_level_created (Postgres B-Tree Backward Scan)
        case OLDEST -> Sort.by(
                Sort.Order.asc("createdAt"),
                Sort.Order.asc("id")
        );
    };
}

private static @NonNull List<CommentMedia> getCommentMedia(List<CommentMediaRequest> mediaRequests, Comment comment) {
    if (mediaRequests == null || mediaRequests.isEmpty()) {
        return List.of();
    }

    List<CommentMedia> mediaList = new ArrayList<>();

    for (int i = 0; i < mediaRequests.size(); i++) {
        CommentMediaRequest mediaReq = mediaRequests.get(i);

        CommentMedia media = new CommentMedia(comment, mediaReq.mediaType(), mediaReq.mediaUrl(), (short) i);

        media.setWidth(mediaReq.width());
        media.setHeight(mediaReq.height());

        mediaList.add(media);
    }
    return mediaList;
}

private List<UserProfile> validateAndGetTaggedProfiles(UUID currentUserId, List<UUID> rawTaggedIds) {
    if (rawTaggedIds == null || rawTaggedIds.isEmpty()) {
        return List.of();
    }

    // 1. Lọc trùng ID và LOẠI BỎ chính mình (không tự tag mình)
    Set<UUID> uniqueIds = new HashSet<>(rawTaggedIds);
    uniqueIds.remove(null);
    uniqueIds.remove(currentUserId);

    if (uniqueIds.isEmpty()) {
        return List.of();
    }

    // 2. Batch query kiểm tra sự tồn tại trong DB (1 query duy nhất)
    List<UserProfile> profiles = userProfileRepository.findAllById(uniqueIds);
    if (profiles.size() != uniqueIds.size()) {
        Set<UUID> foundIds = profiles.stream().map(UserProfile::getUserId).collect(Collectors.toSet());
        for (UUID requestedId : uniqueIds) {
            if (!foundIds.contains(requestedId)) {
                throw new UserNotFoundException(requestedId);
            }
        }
    }

    // 3. Kiểm tra quan hệ chặn (Block 2 chiều)
    for (UserProfile profile : profiles) {
        if (userBlockRepository.existsBlockBetween(currentUserId, profile.getUserId())) {
            throw new IllegalStateException("Cannot tag user with id: " + profile.getUserId() + " due to block restrictions");
        }
    }

    return profiles;
}
}