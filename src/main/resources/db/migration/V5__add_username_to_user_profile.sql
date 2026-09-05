-- V5__add_username_to_user_profile.sql

ALTER TABLE user_profile
    ADD COLUMN username VARCHAR(50) NULL;

CREATE UNIQUE INDEX uq_user_profile_username_lower
    ON user_profile (LOWER(username))
    WHERE username IS NOT NULL;
