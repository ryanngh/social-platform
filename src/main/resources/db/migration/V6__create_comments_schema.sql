-- V6__create_comments_schema.sql

-- =========================================================
-- comments
-- =========================================================

CREATE TABLE comments
(
    id                UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    post_id           UUID           NOT NULL,
    author_id         UUID           NULL,
    parent_comment_id UUID           NULL,

    content           TEXT           NULL,

    like_count        INT            NOT NULL DEFAULT 0,
    reply_count       INT            NOT NULL DEFAULT 0,

    is_pinned         BOOLEAN        NOT NULL DEFAULT FALSE,
    pinned_at         TIMESTAMPTZ(0) NULL,

    created_at        TIMESTAMPTZ(0) NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ(0) NOT NULL DEFAULT now(),
    edited_at         TIMESTAMPTZ(0) NULL,
    deleted_at        TIMESTAMPTZ(0) NULL,

    CONSTRAINT fk_comments_post
        FOREIGN KEY (post_id)
            REFERENCES posts (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_comments_author
        FOREIGN KEY (author_id)
            REFERENCES users (id)
            ON DELETE SET NULL,

    CONSTRAINT fk_comments_parent_comment
        FOREIGN KEY (parent_comment_id)
            REFERENCES comments (id)
            ON DELETE CASCADE,

    CONSTRAINT ck_comments_not_self_parent
        CHECK (
            parent_comment_id IS NULL
            OR parent_comment_id <> id
        ),

    CONSTRAINT ck_comments_content_length
        CHECK (
            content IS NULL
            OR (
                length(btrim(content)) > 0
                AND length(content) <= 10000
            )
        ),

    CONSTRAINT ck_comments_pin_consistency
        CHECK (
            (is_pinned = TRUE AND pinned_at IS NOT NULL)
            OR
            (is_pinned = FALSE AND pinned_at IS NULL)
        ),

    CONSTRAINT ck_comments_pin_top_level_only
        CHECK (
            is_pinned = FALSE
            OR parent_comment_id IS NULL
        ),

    CONSTRAINT ck_comments_counts_non_negative
        CHECK (
            like_count >= 0
            AND reply_count >= 0
        )
);


-- =========================================================
-- Indexes
-- =========================================================

-- 1. Xem theo Phù hợp nhất (Popular)
CREATE INDEX idx_comments_post_top_level_popular
    ON comments (
        post_id,
        like_count DESC,
        reply_count DESC,
        created_at ASC,
        id ASC
    )
    WHERE deleted_at IS NULL
      AND parent_comment_id IS NULL;

-- 2. Xem theo Mới nhất / Cũ nhất (Chronological)
CREATE INDEX idx_comments_post_top_level_created
    ON comments (
        post_id,
        created_at DESC,
        id DESC
    )
    WHERE deleted_at IS NULL
      AND parent_comment_id IS NULL;

-- 3. Phân trang Replies của một bình luận
CREATE INDEX idx_comments_parent_comment_active
    ON comments (
        parent_comment_id,
        created_at ASC,
        id ASC
    )
    WHERE deleted_at IS NULL;

-- 4. Tra cứu bình luận theo Author (Active)
CREATE INDEX idx_comments_author_active
    ON comments (
        author_id,
        created_at DESC
    )
    WHERE deleted_at IS NULL;

-- 5. Ràng buộc: Mỗi bài post chỉ có tối đa 1 comment được ghim
CREATE UNIQUE INDEX uq_comments_post_single_pinned
    ON comments (post_id)
    WHERE deleted_at IS NULL
      AND is_pinned = TRUE;


-- =========================================================
-- Validate parent comment
--
-- - Immutability: Không được đổi post_id / parent_comment_id khi update
-- - Only reply to top-level comment
-- - Parent must belong to same post
-- - Parent must not be deleted
-- =========================================================

CREATE OR REPLACE FUNCTION fn_comments_validate_parent()
RETURNS TRIGGER AS
$$
BEGIN
    -- Không cho phép đổi post_id hoặc chuyển cha
    IF TG_OP = 'UPDATE' THEN
        IF NEW.post_id <> OLD.post_id THEN
            RAISE EXCEPTION 'Cannot change post_id of a comment';
        END IF;

        IF NEW.parent_comment_id IS DISTINCT FROM OLD.parent_comment_id THEN
            RAISE EXCEPTION 'Cannot change parent_comment_id of a comment';
        END IF;
    END IF;

    IF NEW.parent_comment_id IS NOT NULL THEN
        PERFORM 1
        FROM comments
        WHERE id = NEW.parent_comment_id
          AND parent_comment_id IS NULL
          AND post_id = NEW.post_id
          AND deleted_at IS NULL;

        IF NOT FOUND THEN
            RAISE EXCEPTION
                'parent_comment_id must reference an active top-level comment on the same post';
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_comments_validate_parent
    BEFORE INSERT OR UPDATE OF parent_comment_id, post_id
    ON comments
    FOR EACH ROW
EXECUTE FUNCTION fn_comments_validate_parent();


-- =========================================================
-- comment_mentions
-- =========================================================

CREATE TABLE comment_mentions
(
    id                UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    comment_id        UUID           NOT NULL,
    mentioned_user_id UUID           NOT NULL,
    created_at        TIMESTAMPTZ(0) NOT NULL DEFAULT now(),

    CONSTRAINT fk_comment_mentions_comment
        FOREIGN KEY (comment_id)
            REFERENCES comments (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_comment_mentions_mentioned_user
        FOREIGN KEY (mentioned_user_id)
            REFERENCES users (id)
            ON DELETE CASCADE,

    CONSTRAINT uq_comment_mentions_pair
        UNIQUE (comment_id, mentioned_user_id)
);

-- Unique index trên (comment_id, mentioned_user_id) đã cover comment_id,
-- chỉ cần index cho mentioned_user_id để tra cứu ngược (User được tag ở đâu)
CREATE INDEX idx_comment_mentions_mentioned_user_id
    ON comment_mentions (mentioned_user_id);


-- =========================================================
-- comment_media
-- Supports IMAGE / GIF
-- =========================================================

CREATE TABLE comment_media
(
    id            UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    comment_id    UUID           NOT NULL,

    media_type    VARCHAR(20)    NOT NULL,
    media_url     TEXT           NOT NULL,
    width         INTEGER        NULL,
    height        INTEGER        NULL,
    display_order SMALLINT       NOT NULL DEFAULT 0,

    created_at    TIMESTAMPTZ(0) NOT NULL DEFAULT now(),

    CONSTRAINT fk_comment_media_comment
        FOREIGN KEY (comment_id)
            REFERENCES comments (id)
            ON DELETE CASCADE,

    CONSTRAINT ck_comment_media_type
        CHECK (media_type IN ('IMAGE', 'GIF')),

    CONSTRAINT ck_comment_media_display_order
        CHECK (display_order >= 0),

    CONSTRAINT uq_comment_media_order
        UNIQUE (comment_id, display_order)
);
