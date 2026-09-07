package com.ryan.socialplatform.post.service;

import com.ryan.socialplatform.post.dto.*;
import com.ryan.socialplatform.post.entity.*;
import com.ryan.socialplatform.post.enums.PostVisibility;
import com.ryan.socialplatform.post.repository.*;
import com.ryan.socialplatform.relationship.repository.FriendshipRepository;
import com.ryan.socialplatform.relationship.repository.UserBlockRepository;
import com.ryan.socialplatform.user.dto.UserSummaryResponse;
import com.ryan.socialplatform.user.entity.User;
import com.ryan.socialplatform.user.entity.UserProfile;
import com.ryan.socialplatform.user.exceptions.UserNotFoundException;
import com.ryan.socialplatform.user.repository.UserProfileRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PostService {
    private final PostRepository postRepository;
    private final PostMediaRepository postMediaRepository;
    private final PostTagRepository postTagRepository;
    private final HashTagRepository hashtagRepository;
    private final PostHashtagRepository postHashtagRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserBlockRepository userBlockRepository;
    private final FriendshipRepository friendshipRepository;

    public PostService(PostRepository postRepository,
                       PostMediaRepository postMediaRepository,
                       PostTagRepository postTagRepository,
                       HashTagRepository hashtagRepository,
                       PostHashtagRepository postHashtagRepository,
                       UserProfileRepository userProfileRepository,
                       UserBlockRepository userBlockRepository,
                       FriendshipRepository friendshipRepository) {
        this.postRepository = postRepository;
        this.postMediaRepository = postMediaRepository;
        this.postTagRepository = postTagRepository;
        this.hashtagRepository = hashtagRepository;
        this.postHashtagRepository = postHashtagRepository;
        this.userProfileRepository = userProfileRepository;
        this.userBlockRepository = userBlockRepository;
        this.friendshipRepository = friendshipRepository;
    }

    /**
     * Tạo Posts
     */
    @Transactional
    public CreatePostResponse create(UUID currentUserId, CreatePostRequest request) {
        // =====================================================================
        // BƯỚC 1: Validate đầu vào & lấy thông tin Tác giả (Author)
        // =====================================================================
        boolean hasContent = request.content() != null && !request.content().isBlank();
        boolean hasMedia = request.media() != null && !request.media().isEmpty();

        if (!hasContent && !hasMedia) {
            throw new IllegalArgumentException("Post must contain at least text content or media");
        }
        // Lấy Profile của tác giả (chứa cả entity User và avatar, tên hiển thị)
        UserProfile authorProfile = userProfileRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException(currentUserId));
        User author = authorProfile.getUser();

        // =====================================================================
        // BƯỚC 2: Kiểm tra danh sách người được gắn thẻ (Tagged Users)
        // =====================================================================
        List<UserProfile> taggedProfiles = validateAndGetTaggedProfiles(currentUserId, request.taggedUserIds());


        // =====================================================================
        // BƯỚC 3: Tạo và lưu Entity Post
        // =====================================================================
        Post post = new Post(author, request.content(), request.visibility());
        post = postRepository.save(post);

        // =====================================================================
        // BƯỚC 4: Xử lý và lưu Media (ảnh / video)
        // =====================================================================
        List<PostMediaResponse> mediaResponses = List.of();
        if (hasMedia) {
            List<PostMedia> postMediaList = new ArrayList<>();
            List<PostMediaRequest> mediaRequests = request.media();
            for (int i = 0; i < mediaRequests.size(); i++) {
                PostMediaRequest mediaReq = mediaRequests.get(i);
                PostMedia media = new PostMedia(
                        post,
                        mediaReq.mediaType(),
                        mediaReq.mediaUrl(),
                        (short) i // Gán thứ tự hiển thị: 0, 1, 2...
                );
                if (mediaReq.thumbnailUrl() != null && !mediaReq.thumbnailUrl().isBlank()) {
                    media.setThumbnailUrl(mediaReq.thumbnailUrl());
                }
                postMediaList.add(media);
            }
            List<PostMedia> savedMedia = postMediaRepository.saveAll(postMediaList);
            mediaResponses = savedMedia.stream()
                    .map(PostMediaResponse::from)
                    .toList();
        }

        // =====================================================================
        // BƯỚC 5: Xử lý và lưu Hashtags
        // =====================================================================
        List<String> savedHashtagNames = List.of();
        if (request.hashtags() != null && !request.hashtags().isEmpty()) {
            savedHashtagNames = processHashtags(post, request.hashtags());
        }
        // =====================================================================
        // BƯỚC 6: Lưu các bản ghi PostTag
        // =====================================================================
        List<UserSummaryResponse> taggedUsersResponse = List.of();
        if (!taggedProfiles.isEmpty()) {
            Post finalPost = post;
            List<PostTag> postTags = taggedProfiles.stream()
                    .map(profile -> new PostTag(finalPost, profile.getUser()))
                    .toList();
            postTagRepository.saveAll(postTags);
            taggedUsersResponse = taggedProfiles.stream()
                    .map(UserSummaryResponse::from)
                    .toList();
        }

        // =====================================================================
        // BƯỚC 7: Trả về CreatePostResponse
        // =====================================================================
        return CreatePostResponse.of(
                post,
                UserSummaryResponse.from(authorProfile),
                mediaResponses,
                savedHashtagNames,
                taggedUsersResponse
        );
    }

    /**
     * Get Post
     */
    @Transactional(readOnly = true)
    public PostResponse getPost(UUID currentUserId, UUID postId) {
        //1. check post
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post with id " + postId + " does not exist"));
        UUID authorId = post.getAuthor().getId();

        //2. check nếu là chính chủ -> return luôn

        boolean isAuthor = authorId.equals(currentUserId);
        if (!isAuthor) {
            //3. check user_block
            if (userBlockRepository.existsBlockBetween(currentUserId, authorId)) {
                throw new IllegalArgumentException();
            }
            //4. check visibility
            switch (post.getVisibility()) {
                case PUBLIC -> {
                    // PUBLIC
                }
                case FRIENDS -> {
                    boolean areFriends = friendshipRepository.areFriends(currentUserId, authorId);
                    if (!areFriends) {
                        //TODO: throw new
                    }
                }
                case CLOSE_FRIENDS -> {
                    // TODO: missed module Close Friends
                }
                case PRIVATE -> {
                    // TODO: PostNotFoundException
                }
            }
        }

        return toPostResponse(post);
    }

    /**
     * Newfeeds
     */
    @Transactional(readOnly = true)
    public Slice<PostResponse> newFeeds(UUID userId, Pageable pageable) {
        //TODO newFeeds
        return null;
    }

    /**
     * get user posts (profile)
     */
    @Transactional(readOnly = true)
    public Slice<PostResponse> getUserPost(UUID targetUserId, UUID currentUserId, Pageable pageable) {
        // check targetUserId
        if (!userProfileRepository.existsById(targetUserId)) {
            throw new UserNotFoundException(targetUserId);
        }
        // check blockuser
        if (userBlockRepository.existsBlockBetween(currentUserId, targetUserId)) {
            return new SliceImpl<>(List.of(), pageable, false);
        }
        // check visibilities
        Set<PostVisibility> allowedVisibilities = new HashSet<>();
        boolean isOwner = targetUserId.equals(currentUserId);

        if (isOwner) {
            // Tự xem trang cá nhân của mình -> Thấy TẤT CẢ các bài
            allowedVisibilities.addAll(List.of(
                    PostVisibility.PUBLIC,
                    PostVisibility.FRIENDS,
                    PostVisibility.CLOSE_FRIENDS,
                    PostVisibility.PRIVATE
            ));
        } else {
            // Xem trang của người khác:
            // Luôn thấy bài PUBLIC
            allowedVisibilities.add(PostVisibility.PUBLIC);
            // Nếu là bạn bè -> Thấy thêm bài FRIENDS
            boolean areFriends = friendshipRepository
                    .areFriends(currentUserId, targetUserId);
            if (areFriends) {
                allowedVisibilities.add(PostVisibility.FRIENDS);
            }
        }
        Slice<Post> postsSlice = postRepository.findProfilePosts(targetUserId, allowedVisibilities, pageable);
        return toPostResponseSlice(postsSlice);
    }

    @Transactional
    public UpdatePostResponse updatePost(UUID currentUserId, UUID postId, UpdatePostRequest request) {
        //1: Kiểm tra post tồn tại và chưa bị soft-delete
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post with id " + postId + " does not exist"));

        //2: Author check
        if (!post.getAuthor().getId().equals(currentUserId)) {
            throw new IllegalStateException("You are not authorized to update this post");
        }

        // 3.1: check final content
        String updatedContent = post.getContent();
        if (request.content() != null) {
            String trimmed = request.content().trim();
            updatedContent = trimmed.isEmpty() ? null : trimmed;
        }
        boolean willHaveContent = updatedContent != null && !updatedContent.isBlank();

        // 3.2 check final media
        boolean willHaveMedia;
        if (request.media() != null) {
            willHaveMedia = !request.media().isEmpty();
        } else {
            willHaveMedia = postMediaRepository.existsByPostId(postId);
        }

        if (!willHaveContent && !willHaveMedia) {
            throw new IllegalArgumentException("Post must contain at least text content or media");
        }

        //4: update Entity Post (content & visibility)
        if (request.content() != null) {
            post.setContent(updatedContent);
        }
        if (request.visibility() != null && request.visibility() != post.getVisibility()) {
            post.setVisibility(request.visibility());
        }

        //5: update Media (ảnh / video)
        List<PostMediaResponse> mediaResponses;
        if (request.media() != null) {
            postMediaRepository.deleteByPostId(postId);

            if (!request.media().isEmpty()) {
                List<PostMedia> postMediaList = new ArrayList<>();
                List<PostMediaRequest> mediaRequests = request.media();
                for (int i = 0; i < mediaRequests.size(); i++) {
                    PostMediaRequest mediaReq = mediaRequests.get(i);
                    PostMedia media = new PostMedia(
                            post,
                            mediaReq.mediaType(),
                            mediaReq.mediaUrl(),
                            (short) i
                    );
                    if (mediaReq.thumbnailUrl() != null && !mediaReq.thumbnailUrl().isBlank()) {
                        media.setThumbnailUrl(mediaReq.thumbnailUrl());
                    }
                    postMediaList.add(media);
                }
                List<PostMedia> savedMedia = postMediaRepository.saveAll(postMediaList);
                mediaResponses = savedMedia.stream()
                        .map(PostMediaResponse::from)
                        .toList();
            } else {
                mediaResponses = List.of();
            }
        } else {
            mediaResponses = postMediaRepository.findAllByPostIdOrderByDisplayOrderAsc(postId)
                    .stream()
                    .map(PostMediaResponse::from)
                    .toList();
        }

        // BƯỚC 6: Cập nhật Hashtags
        List<String> savedHashtagNames;
        if (request.hashtags() != null) {
            postHashtagRepository.deleteByPostId(postId);

            if (!request.hashtags().isEmpty()) {
                savedHashtagNames = processHashtags(post, request.hashtags());
            } else {
                savedHashtagNames = List.of();
            }
        } else {
            savedHashtagNames = postHashtagRepository.findHashtagNamesByPostId(postId);
        }

        // BƯỚC 7: Cập nhật Tagged Users
        List<UserSummaryResponse> taggedUsersResponse;
        if (request.taggedUserIds() != null) {
            postTagRepository.deleteByPostId(postId);

            if (!request.taggedUserIds().isEmpty()) {
                List<UserProfile> taggedProfiles = validateAndGetTaggedProfiles(currentUserId, request.taggedUserIds());
                List<PostTag> postTags = taggedProfiles.stream()
                        .map(profile -> new PostTag(post, profile.getUser()))
                        .toList();
                postTagRepository.saveAll(postTags);
                taggedUsersResponse = taggedProfiles.stream()
                        .map(UserSummaryResponse::from)
                        .toList();
            } else {
                taggedUsersResponse = List.of();
            }
        } else {
            List<UUID> taggedUserIds = postTagRepository.findTaggedUserIdsByPostId(postId);
            taggedUsersResponse = taggedUserIds.isEmpty()
                    ? List.of()
                    : userProfileRepository.findAllById(taggedUserIds).stream()
                    .map(UserSummaryResponse::from)
                    .toList();
        }

        // BƯỚC 8: Lấy Profile Tác giả & trả về UpdatePostResponse
        UserProfile authorProfile = userProfileRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException(currentUserId));

        return UpdatePostResponse.of(
                post,
                UserSummaryResponse.from(authorProfile),
                mediaResponses,
                savedHashtagNames,
                taggedUsersResponse
        );
    }

    /**
     * Xóa bài viết (Soft Delete)
     */
    @Transactional
    public void deletePost(UUID currentUserId, UUID postId) {
        // 1. Kiểm tra bài viết tồn tại và chưa bị xóa mềm
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post with id " + postId + " does not exist"));

        // 2. Kiểm tra quyền sở hữu (Chỉ tác giả mới có quyền xóa bài viết)
        if (!post.getAuthor().getId().equals(currentUserId)) {
            throw new IllegalStateException("You are not authorized to delete this post");
        }

        // 3. Thực hiện xóa mềm
        post.markDeleted();
    }

    /**
     * Khôi phục bài viết đã xóa mềm
     */
    @Transactional
    public void restorePost(UUID currentUserId, UUID postId) {
        // 1. Tìm bài viết theo ID (kể cả đã bị xóa mềm)
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post with id " + postId + " does not exist"));

        // 2. Kiểm tra quyền sở hữu
        if (!post.getAuthor().getId().equals(currentUserId)) {
            throw new IllegalStateException("You are not authorized to restore this post");
        }

        // 3. Kiểm tra xem bài viết có đang trong trạng thái bị xóa không
        if (!post.isDeleted()) {
            throw new IllegalStateException("Post is not deleted");
        }

        // 4. Khôi phục bài viết
        post.restore();
    }

    /**
     * Helper tối ưu hiệu năng: Batch fetch tất cả dữ liệu liên quan (Media, Hashtag, Tag, Author)
     * Triệt tiêu hoàn toàn vấn đề N+1 Query khi tải danh sách bài viết.
     */
    private Slice<PostResponse> toPostResponseSlice(Slice<Post> postsSlice) {
        List<Post> posts = postsSlice.getContent();
        if (posts.isEmpty()) {
            return postsSlice.map(p -> null);
        }

        List<UUID> postIds = posts.stream().map(Post::getId).toList();

        // 1. Batch query thông tin tác giả của các bài viết (1 query)
        Set<UUID> authorIds = posts.stream()
                .map(p -> p.getAuthor().getId())
                .collect(Collectors.toSet());
        Map<UUID, UserProfile> authorProfileMap = userProfileRepository.findAllById(authorIds)
                .stream()
                .collect(Collectors.toMap(UserProfile::getUserId, Function.identity()));

        // 2. Batch query Media (1 query duy nhất cho tất cả bài viết)
        Map<UUID, List<PostMediaResponse>> mediaMap = postMediaRepository
                .findAllByPostIdInOrderByDisplayOrderAsc(postIds)
                .stream()
                .collect(Collectors.groupingBy(
                        m -> m.getPost().getId(),
                        Collectors.mapping(PostMediaResponse::from, Collectors.toList())
                ));

        // 3. Batch query Hashtags (1 query JOIN FETCH duy nhất)
        Map<UUID, List<String>> hashtagMap = postHashtagRepository
                .findAllByPostIdInWithHashtag(postIds)
                .stream()
                .collect(Collectors.groupingBy(
                        ph -> ph.getPost().getId(),
                        Collectors.mapping(ph -> ph.getHashtag().getTag(), Collectors.toList())
                ));

        // 4. Batch query Tagged Users (2 query duy nhất: 1 cho post_tags, 1 cho user_profile)
        List<PostTag> allPostTags = postTagRepository.findAllByPostIdInWithTaggedUser(postIds);
        Set<UUID> allTaggedUserIds = allPostTags.stream()
                .map(pt -> pt.getTaggedUser().getId())
                .collect(Collectors.toSet());

        Map<UUID, UserProfile> taggedProfileMap = allTaggedUserIds.isEmpty()
                ? Map.of()
                : userProfileRepository.findAllById(allTaggedUserIds)
                .stream()
                .collect(Collectors.toMap(UserProfile::getUserId, Function.identity()));

        Map<UUID, List<UserSummaryResponse>> taggedUsersMap = allPostTags.stream()
                .collect(Collectors.groupingBy(
                        pt -> pt.getPost().getId(),
                        Collectors.mapping(
                                pt -> UserSummaryResponse.from(taggedProfileMap.get(pt.getTaggedUser().getId())),
                                Collectors.toList()
                        )
                ));

        // 5. Map in-memory cực nhanh O(1)
        return postsSlice.map(post -> {
            UUID postId = post.getId();
            UserProfile authorProfile = authorProfileMap.get(post.getAuthor().getId());

            return PostResponse.of(
                    post,
                    UserSummaryResponse.from(authorProfile),
                    mediaMap.getOrDefault(postId, List.of()),
                    hashtagMap.getOrDefault(postId, List.of()),
                    taggedUsersMap.getOrDefault(postId, List.of()),
                    0L, // reactionCount
                    0L  // commentCount
            );
        });
    }

    /**
     * HELPER
     */
    private PostResponse toPostResponse(Post post) {
        UUID postId = post.getId();
        UUID authorId = post.getAuthor().getId();

        // 1. Profile tác giả
        UserProfile authorProfile = userProfileRepository.findById(authorId).orElse(null);

        // 2. Media
        List<PostMediaResponse> mediaResponses = postMediaRepository
                .findAllByPostIdOrderByDisplayOrderAsc(postId)
                .stream()
                .map(PostMediaResponse::from)
                .toList();

        // 3. Hashtag
        List<String> hashtags = postHashtagRepository.findHashtagNamesByPostId(postId);

        // 4. Tagged Users
        List<UUID> taggedUserIds = postTagRepository.findTaggedUserIdsByPostId(postId);
        List<UserSummaryResponse> taggedUsers = taggedUserIds.isEmpty()
                ? List.of()
                : userProfileRepository.findAllById(taggedUserIds).stream()
                .map(UserSummaryResponse::from)
                .toList();

        return PostResponse.of(
                post,
                UserSummaryResponse.from(authorProfile),
                mediaResponses,
                hashtags,
                taggedUsers,
                0L, // reactionCount
                0L  // commentCount
        );
    }

    private List<UserProfile> validateAndGetTaggedProfiles(UUID currentUserId, List<UUID> rawTaggedIds) {
        if (rawTaggedIds == null || rawTaggedIds.isEmpty()) {
            return List.of();
        }
        // 1. Loại bỏ ID trùng
        Set<UUID> uniqueIds = new HashSet<>(rawTaggedIds);
        // 2. Không cho phép tự tag chính mình
        if (uniqueIds.contains(currentUserId)) {
            throw new IllegalArgumentException("You cannot tag yourself in a post");
        }
        // 3. Batch query kiểm tra sự tồn tại trong DB (1 query duy nhất)
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
        // 4. Kiểm tra quan hệ chặn (Block 2 chiều)
        for (UserProfile profile : profiles) {
            if (userBlockRepository.existsBlockBetween(currentUserId, profile.getUserId())) {
                throw new IllegalStateException("Cannot tag user with id: " + profile.getUserId() + " due to block restrictions");
            }
        }
        return profiles;
    }

    private List<String> processHashtags(Post post, List<String> rawHashtags) {
        // Chuẩn hóa: xóa khoảng trắng, xóa dấu '#' ở đầu, đưa về chữ thường và lọc trùng
        Set<String> cleanTagNames = rawHashtags.stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .map(tag -> tag.trim().replaceFirst("^#", "").toLowerCase())
                .filter(tag -> !tag.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (cleanTagNames.isEmpty()) {
            return List.of();
        }
        List<PostHashTag> postHashtags = new ArrayList<>();
        List<String> resultTagNames = new ArrayList<>();
        for (String tagName : cleanTagNames) {
            // Tìm trong DB xem hashtag đã từng có chưa, chưa có thì tạo mới
            HashTag hashtag = hashtagRepository.findByTag(tagName)
                    .orElseGet(() -> hashtagRepository.save(new HashTag(tagName)));
            postHashtags.add(new PostHashTag(post, hashtag));
            resultTagNames.add(tagName);
        }
        postHashtagRepository.saveAll(postHashtags);
        return resultTagNames;
    }
}
