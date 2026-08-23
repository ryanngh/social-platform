-- users

CREATE TABLE users
(
    id          UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    status      VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    is_verified BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ(0) NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ(0) NOT NULL DEFAULT now(),

    CONSTRAINT ck_users_status
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DEACTIVATED', 'DELETED'))
);


-- user_credentials

CREATE TABLE user_credentials
(
    user_id              UUID PRIMARY KEY,
    email                VARCHAR(255)   NULL,
    phone_number         VARCHAR(20)    NOT NULL,
    password_hash        TEXT           NOT NULL,
    mfa_enabled          BOOLEAN        NOT NULL DEFAULT FALSE,
    mfa_secret_encrypted TEXT           NULL,
    failed_login_count   SMALLINT       NOT NULL DEFAULT 0,
    locked_until         TIMESTAMPTZ(0) NULL,
    password_changed_at  TIMESTAMPTZ(0) NULL,
    email_verified_at    TIMESTAMPTZ(0) NULL,
    phone_verified_at    TIMESTAMPTZ(0) NULL,
    is_deleted           BOOLEAN        NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_user_credentials_user_id
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE,

    CONSTRAINT ck_user_credentials_failed_login_count
        CHECK (failed_login_count >= 0)
);

CREATE UNIQUE INDEX uq_user_credentials_email_active
    ON user_credentials (email)
    WHERE email IS NOT NULL
        AND is_deleted = FALSE;

CREATE UNIQUE INDEX uq_user_credentials_phone_active
    ON user_credentials (phone_number)
    WHERE is_deleted = FALSE;


-- user_profile

CREATE TABLE user_profile
(
    user_id       UUID PRIMARY KEY,
    first_name    VARCHAR(50)  NOT NULL,
    last_name     VARCHAR(50)  NOT NULL,
    avatar_url    TEXT         NULL,
    banner_url    TEXT         NULL,
    bio           TEXT         NULL,
    pronouns      VARCHAR(50)  NULL,
    location      VARCHAR(100) NULL,
    website_url   VARCHAR(500) NULL,
    birthday      DATE         NULL,
    pronunciation VARCHAR(100) NULL,

    CONSTRAINT fk_user_profile_user_id
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE
);


-- user_state

CREATE TABLE user_state
(
    user_id             UUID PRIMARY KEY,
    presence            VARCHAR(10)    NOT NULL DEFAULT 'OFFLINE',
    custom_status_text  VARCHAR(128)   NULL,
    custom_status_emoji VARCHAR(50)    NULL,
    last_seen_at        TIMESTAMPTZ(0) NULL,
    active_device       VARCHAR(10)    NULL,
    updated_at          TIMESTAMPTZ(0) NOT NULL DEFAULT now(),

    CONSTRAINT fk_user_state_user_id
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE,

    CONSTRAINT ck_user_state_presence
        CHECK (presence IN ('ONLINE', 'IDLE', 'DND', 'OFFLINE')),

    CONSTRAINT ck_user_state_active_device
        CHECK (active_device IN ('DESKTOP', 'MOBILE', 'WEB'))
);


-- user_sessions

CREATE TABLE user_sessions
(
    id                 UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    user_id            UUID           NOT NULL,
    refresh_token_hash VARCHAR(64)    NOT NULL,
    device_name        VARCHAR(100)   NULL,
    user_agent         TEXT           NULL,
    ip_address         VARCHAR(45)    NULL,
    created_at         TIMESTAMPTZ(0) NOT NULL DEFAULT now(),
    last_active_at     TIMESTAMPTZ(0) NULL,
    expires_at         TIMESTAMPTZ(0) NOT NULL,
    revoked_at         TIMESTAMPTZ(0) NULL,

    CONSTRAINT fk_user_sessions_user_id
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE,

    CONSTRAINT uq_user_sessions_refresh_token_hash
        UNIQUE (refresh_token_hash)
);

CREATE INDEX idx_user_sessions_user_id
    ON user_sessions (user_id);


-- verification_tokens

CREATE TABLE verification_tokens
(
    token_hash VARCHAR(64) PRIMARY KEY,
    user_id    UUID           NOT NULL,
    type       VARCHAR(20)    NOT NULL,
    created_at TIMESTAMPTZ(0) NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ(0) NOT NULL,
    used_at    TIMESTAMPTZ(0) NULL,

    CONSTRAINT fk_verification_tokens_user_id
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE,

    CONSTRAINT ck_verification_tokens_type
        CHECK (
            type IN (
                     'VERIFY_EMAIL',
                     'VERIFY_PHONE',
                     'RESET_PASSWORD',
                     'MAGIC_LINK'
                )
            )
);

CREATE INDEX idx_verification_tokens_user_id
    ON verification_tokens (user_id);

CREATE UNIQUE INDEX uq_verification_tokens_active
    ON verification_tokens (user_id, type)
    WHERE used_at IS NULL;


-- user_app_roles

CREATE TABLE user_app_roles
(
    id         UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    user_id    UUID           NOT NULL,
    role       VARCHAR(20)    NOT NULL,
    granted_by UUID           NULL,
    granted_at TIMESTAMPTZ(0) NOT NULL DEFAULT now(),

    CONSTRAINT fk_user_app_roles_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_user_app_roles_granted_by
        FOREIGN KEY (granted_by)
            REFERENCES users (id)
            ON DELETE SET NULL,

    CONSTRAINT ck_user_app_roles_role
        CHECK (role IN ('USER', 'MODERATOR', 'ADMIN', 'STAFF')),

    CONSTRAINT uq_user_app_roles
        UNIQUE (user_id, role)
);

CREATE INDEX idx_user_app_roles_user_id
    ON user_app_roles (user_id);

CREATE INDEX idx_user_app_roles_granted_by
    ON user_app_roles (granted_by);