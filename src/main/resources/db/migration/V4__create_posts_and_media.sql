-- V4__create_posts_and_content.sql

-- posts

CREATE TABLE posts
(
    id          UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    author_id   UUID           NOT NULL,
    content     TEXT           NULL,
    visibility  VARCHAR(20)    NOT NULL DEFAULT 'PUBLIC',
    created_at  TIMESTAMPTZ(0) NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ(0) NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ(0) NULL,

    CONSTRAINT fk_posts_author
        FOREIGN KEY (author_id)
            REFERENCES users (id)
            ON DELETE CASCADE,

    CONSTRAINT ck_posts_visibility
        CHECK (visibility IN ('PUBLIC', 'FRIENDS', 'CLOSE_FRIENDS', 'PRIVATE'))
);

CREATE INDEX idx_posts_author_id_active
    ON posts (author_id, created_at DESC)
    WHERE deleted_at IS NULL;


-- post_media

CREATE TABLE post_media
(
    id                UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    post_id           UUID           NOT NULL,
    media_type        VARCHAR(20)    NOT NULL,
    media_url         TEXT           NOT NULL,
    thumbnail_url     TEXT           NULL,
    width             SMALLINT       NULL,
    height            SMALLINT       NULL,
    duration_seconds  SMALLINT       NULL,
    file_size_bytes   BIGINT         NULL,
    display_order     SMALLINT       NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ(0) NOT NULL DEFAULT now(),

    CONSTRAINT fk_post_media_post
        FOREIGN KEY (post_id)
            REFERENCES posts (id)
            ON DELETE CASCADE,

    CONSTRAINT ck_post_media_type
        CHECK (media_type IN ('IMAGE', 'VIDEO', 'GIF'))
);

CREATE INDEX idx_post_media_post_id
    ON post_media (post_id);

CREATE UNIQUE INDEX uq_post_media_post_order
    ON post_media (post_id, display_order);


-- hashtags

CREATE TABLE hashtags
(
    id          UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    tag         VARCHAR(100)   NOT NULL,
    created_at  TIMESTAMPTZ(0) NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_hashtags_tag_lower
    ON hashtags (LOWER(tag));


-- post_hashtags

CREATE TABLE post_hashtags
(
    id           UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    post_id      UUID           NOT NULL,
    hashtag_id   UUID           NOT NULL,
    created_at   TIMESTAMPTZ(0) NOT NULL DEFAULT now(),

    CONSTRAINT fk_post_hashtags_post
        FOREIGN KEY (post_id)
            REFERENCES posts (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_post_hashtags_hashtag
        FOREIGN KEY (hashtag_id)
            REFERENCES hashtags (id)
            ON DELETE CASCADE,

    CONSTRAINT uq_post_hashtags_pair
        UNIQUE (post_id, hashtag_id)
);

CREATE INDEX idx_post_hashtags_post_id
    ON post_hashtags (post_id);

CREATE INDEX idx_post_hashtags_hashtag_id
    ON post_hashtags (hashtag_id);


-- post_tags

CREATE TABLE post_tags
(
    id              UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    post_id         UUID           NOT NULL,
    tagged_user_id  UUID           NOT NULL,
    created_at      TIMESTAMPTZ(0) NOT NULL DEFAULT now(),

    CONSTRAINT fk_post_tags_post
        FOREIGN KEY (post_id)
            REFERENCES posts (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_post_tags_tagged_user
        FOREIGN KEY (tagged_user_id)
            REFERENCES users (id)
            ON DELETE CASCADE,

    CONSTRAINT uq_post_tags_pair
        UNIQUE (post_id, tagged_user_id)
);

CREATE INDEX idx_post_tags_post_id
    ON post_tags (post_id);

CREATE INDEX idx_post_tags_tagged_user_id
    ON post_tags (tagged_user_id);