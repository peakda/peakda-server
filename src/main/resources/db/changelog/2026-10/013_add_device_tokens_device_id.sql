--liquibase formatted sql

--changeset peakda:20261018-013-add-device-tokens-device-id
ALTER TABLE device_tokens
    ADD COLUMN device_id TEXT;

CREATE INDEX ix_device_tokens_user_id_device_id ON device_tokens (user_id, device_id);

COMMENT ON COLUMN device_tokens.device_id IS '로그인 세션과 같은 기기 식별자(auth_sessions.device_id). 그 기기가 로그아웃·해제되면 토큰을 지운다. 구버전 앱은 NULL';
--rollback DROP INDEX ix_device_tokens_user_id_device_id; ALTER TABLE device_tokens DROP COLUMN device_id;
