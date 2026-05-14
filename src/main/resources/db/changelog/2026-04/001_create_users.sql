--liquibase formatted sql logicalFilePath:db/changelog/v1.0.0/001_create_users.sql

--changeset peakda:001-create-users
CREATE TABLE users (
    id                BIGSERIAL   PRIMARY KEY,
    provider          TEXT        NOT NULL,
    provider_id       TEXT        NOT NULL,
    email             TEXT,
    nickname          TEXT        NOT NULL,
    profile_image_url TEXT,
    status            TEXT        NOT NULL DEFAULT 'ACTIVE',
    created_at        TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_users_provider_provider_id UNIQUE (provider, provider_id),
    CONSTRAINT uk_users_nickname UNIQUE (nickname),
    CONSTRAINT ck_users_nickname_length CHECK (
        char_length(nickname) BETWEEN 2 AND 10
    )
);

CREATE INDEX idx_users_status ON users (status);
--rollback DROP TABLE users;
