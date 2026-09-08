package com.ryan.socialplatform.comment.service;

import com.ryan.socialplatform.comment.dto.CommentMediaRequest;
import com.ryan.socialplatform.comment.dto.CommentMediaResponse;
import com.ryan.socialplatform.comment.dto.CommentResponse;
import com.ryan.socialplatform.comment.dto.CreateCommentRequest;
import com.ryan.socialplatform.comment.entity.Comment;
import com.ryan.socialplatform.comment.entity.CommentMedia;
import com.ryan.socialplatform.comment.entity.CommentMention;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
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

    public CommentService(
            CommentRepository commentRepository,
            CommentMediaRepository commentMediaRepository,
            CommentMentionRepository commentMentionRepository,
            PostRepository postRepository,
            UserBlockRepository userBlockRepository,
            FriendshipRepository friendshipRepository,
            UserProfileRepository userProfileRepository) {
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
    public CommentResponse create(
            UUID currentUserId,
            UUID postId,
            CreateCommentRequest request
    ) {
        // 1. Validate input
        boolean hasContent = request.content() != null
                && !request.content().isBlank();

        boolean hasMedia = request.media() != null
                && !request.media().isEmpty();

        if (!hasContent && !hasMedia) {
            throw new IllegalArgumentException(
                    "Comment must contain content or media"
            );
            // TODO: CommentContentRequiredException
        }

        // 2. Visibility & Block
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Post with id " + postId + " does not exist"
                        )
                );
        // TODO: PostNotFoundException

        UUID authorId = post.getAuthor().getId();

        // Không cho comment nếu đang block nhau
        if (!currentUserId.equals(authorId)
                && userBlockRepository.existsBlockBetween(currentUserId, authorId)) {
            throw new IllegalArgumentException(
                    "You cannot comment on this post"
            );
            // TODO: UserBlockedException
        }

        switch (post.getVisibility()) {
            case PUBLIC -> {
                // Anyone can comment
            }

            case FRIENDS -> {
                if (!currentUserId.equals(authorId) && !friendshipRepository.areFriends(currentUserId, authorId)) {
                    throw new IllegalArgumentException(
                            "You must be friends with the post author to comment"
                    );
                }
            }

            case CLOSE_FRIENDS -> {
                // TODO: Implement Close Friends module
            }

            case PRIVATE -> {
                if (!currentUserId.equals(authorId)) {
                    throw new IllegalArgumentException(
                            "You cannot comment on this post"
                    );
                }
            }
        }

        // 3. Validate mentions
        List<UserProfile> taggedProfiles =
                validateAndGetTaggedProfiles(
                        currentUserId,
                        request.mentionedUserIds()
                );

        // 4. Get current user's profile
        UserProfile authorProfile = userProfileRepository
                .findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException(currentUserId));

        User author = authorProfile.getUser();

        // 5. Create and save comment
        Comment comment = new Comment(
                post,
                author,
                null,
                request.content()
        );

        comment = commentRepository.save(comment);

        // 6. Save comment media
        List<CommentMediaResponse> mediaResponses = List.of();

        if (hasMedia) {
            List<CommentMedia> mediaList = getCommentMedia(request, comment);

            mediaResponses = commentMediaRepository
                    .saveAll(mediaList)
                    .stream()
                    .map(CommentMediaResponse::from)
                    .toList();
        }

        // 7. Save mentions
        List<UserSummaryResponse> mentionResponses = List.of();

        if (!taggedProfiles.isEmpty()) {
            Comment finalComment = comment;
            List<CommentMention> commentMentions = taggedProfiles.stream()
                    .map(profile ->
                            new CommentMention(finalComment, profile.getUser())
                    )
                    .toList();

            commentMentionRepository.saveAll(commentMentions);

            mentionResponses = taggedProfiles.stream()
                    .map(UserSummaryResponse::from)
                    .toList();
        }

        // 8. Return response
        return CommentResponse.of(
                comment.getId(),
                post.getId(),
                null,
                UserSummaryResponse.from(authorProfile),
                comment.getContent(),
                mediaResponses,
                mentionResponses,
                comment.getLikeCount(),
                comment.getReplyCount(),
                comment.isPinned(),
                comment.getPinnedAt(),
                comment.getCreatedAt(),
                comment.getEditedAt(),
                comment.getUpdatedAt()
        );
    }

    private static @NonNull List<CommentMedia> getCommentMedia(CreateCommentRequest request, Comment comment) {
        List<CommentMedia> mediaList = new ArrayList<>();

        List<CommentMediaRequest> mediaRequests = request.media();

        for (int i = 0; i < mediaRequests.size(); i++) {
            CommentMediaRequest mediaReq = mediaRequests.get(i);

            CommentMedia media = new CommentMedia(
                    comment,
                    mediaReq.mediaType(),
                    mediaReq.mediaUrl(),
                    (short) i
            );

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
            Set<UUID> foundIds = profiles.stream()
                    .map(UserProfile::getUserId)
                    .collect(Collectors.toSet());
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