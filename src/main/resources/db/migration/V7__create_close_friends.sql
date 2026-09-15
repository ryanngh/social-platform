CREATE TABLE close_friends
(
    id         UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    user_id    UUID           NOT NULL,
    friend_id  UUID           NOT NULL,
    created_at TIMESTAMPTZ(0) NOT NULL DEFAULT now(),

    CONSTRAINT fk_close_friends_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_close_friends_friend
        FOREIGN KEY (friend_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_close_friends_not_self
        CHECK (user_id <> friend_id),
    CONSTRAINT uq_close_friends_pair
        UNIQUE (user_id, friend_id)
);

CREATE INDEX idx_close_friends_user_id ON close_friends (user_id);
CREATE INDEX idx_close_friends_friend_id ON close_friends (friend_id);