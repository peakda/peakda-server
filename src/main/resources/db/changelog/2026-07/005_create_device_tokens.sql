--liquibase formatted sql

--changeset peakda:20260718-005-create-device-tokens
CREATE TABLE device_tokens (
    id         BIGSERIAL   PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    token      TEXT        NOT NULL,
    platform   TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_device_tokens_token UNIQUE (token)
);

CREATE INDEX ix_device_tokens_user_id ON device_tokens (user_id);
--rollback DROP TABLE device_tokens;

--changeset peakda:20260718-005-comment-device-tokens
COMMENT ON TABLE device_tokens IS '사용자 푸시 알림 디바이스 토큰';
COMMENT ON COLUMN device_tokens.id IS '디바이스 토큰 PK';
COMMENT ON COLUMN device_tokens.user_id IS '디바이스 토큰 소유자 사용자 id';
COMMENT ON COLUMN device_tokens.token IS 'FCM 또는 APNs 디바이스 토큰';
COMMENT ON COLUMN device_tokens.platform IS '디바이스 플랫폼 (IOS, ANDROID)';
COMMENT ON COLUMN device_tokens.created_at IS '레코드 생성 시각';
COMMENT ON COLUMN device_tokens.updated_at IS '레코드 최종 수정 시각';
--rollback COMMENT ON TABLE device_tokens IS NULL;
--rollback COMMENT ON COLUMN device_tokens.id IS NULL;
--rollback COMMENT ON COLUMN device_tokens.user_id IS NULL;
--rollback COMMENT ON COLUMN device_tokens.token IS NULL;
--rollback COMMENT ON COLUMN device_tokens.platform IS NULL;
--rollback COMMENT ON COLUMN device_tokens.created_at IS NULL;
--rollback COMMENT ON COLUMN device_tokens.updated_at IS NULL;
