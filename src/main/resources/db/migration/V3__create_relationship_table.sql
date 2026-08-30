-- friend_requests

CREATE TABLE friend_requests
(
    id            UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    sender_id     UUID           NOT NULL,
    receiver_id   UUID           NOT NULL,
    status        VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMPTZ(0) NOT NULL DEFAULT now(),
    responded_at  TIMESTAMPTZ(0) NULL,

    CONSTRAINT fk_friend_requests_sender
        FOREIGN KEY (sender_id)
            REFERENCES users (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_friend_requests_receiver
        FOREIGN KEY (receiver_id)
            REFERENCES users (id)
            ON DELETE CASCADE,

    CONSTRAINT ck_friend_requests_status
        CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'CANCELLED')),

    CONSTRAINT ck_friend_requests_not_self
        CHECK (sender_id <> receiver_id)
);

CREATE INDEX idx_friend_requests_sender_id
    ON friend_requests (sender_id);

CREATE INDEX idx_friend_requests_receiver_id
    ON friend_requests (receiver_id);

CREATE UNIQUE INDEX uq_friend_requests_pending_pair
    ON friend_requests (LEAST(sender_id, receiver_id), GREATEST(sender_id, receiver_id))
    WHERE status = 'PENDING';


-- friendships

CREATE TABLE friendships
(
    id          UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    user_id_1   UUID           NOT NULL,
    user_id_2   UUID           NOT NULL,
    created_at  TIMESTAMPTZ(0) NOT NULL DEFAULT now(),

    CONSTRAINT fk_friendships_user_1
        FOREIGN KEY (user_id_1)
            REFERENCES users (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_friendships_user_2
        FOREIGN KEY (user_id_2)
            REFERENCES users (id)
            ON DELETE CASCADE,

    CONSTRAINT ck_friendships_ordered_pair
        CHECK (user_id_1 < user_id_2),

    CONSTRAINT uq_friendships_pair
        UNIQUE (user_id_1, user_id_2)
);

CREATE INDEX idx_friendships_user_id_1
    ON friendships (user_id_1);

CREATE INDEX idx_friendships_user_id_2
    ON friendships (user_id_2);


-- user_blocks

CREATE TABLE user_blocks
(
    id          UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    blocker_id  UUID           NOT NULL,
    blocked_id  UUID           NOT NULL,
    created_at  TIMESTAMPTZ(0) NOT NULL DEFAULT now(),

    CONSTRAINT fk_user_blocks_blocker
        FOREIGN KEY (blocker_id)
            REFERENCES users (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_user_blocks_blocked
        FOREIGN KEY (blocked_id)
            REFERENCES users (id)
            ON DELETE CASCADE,

    CONSTRAINT ck_user_blocks_not_self
        CHECK (blocker_id <> blocked_id),

    CONSTRAINT uq_user_blocks_pair
        UNIQUE (blocker_id, blocked_id)
);

CREATE INDEX idx_user_blocks_blocker_id
    ON user_blocks (blocker_id);

CREATE INDEX idx_user_blocks_blocked_id
    ON user_blocks (blocked_id);