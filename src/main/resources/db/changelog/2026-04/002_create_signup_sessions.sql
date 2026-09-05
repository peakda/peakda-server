--liquibase formatted sql logicalFilePath:db/changelog/v1.0.0/002_create_signup_sessions.sql

--changeset peakda:002-create-signup-sessions
CREATE TABLE signup_sessions (
    id                BIGSERIAL   PRIMARY KEY,
    token             TEXT        NOT NULL,
    provider          TEXT        NOT NULL,
    provider_id       TEXT        NOT NULL,
    email             TEXT,
    profile_image_url TEXT,
    expires_at        TIMESTAMPTZ NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_signup_sessions_token UNIQUE (token),
    CONSTRAINT uk_signup_sessions_provider_provider_id UNIQUE (provider, provider_id)
);

CREATE INDEX idx_signup_sessions_expires_at ON signup_sessions (expires_at);
--rollback DROP TABLE signup_sessions;
