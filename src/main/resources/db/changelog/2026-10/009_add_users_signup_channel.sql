--liquibase formatted sql

--changeset peakda:20261005-009-add-users-signup-channel
ALTER TABLE users
    ADD COLUMN signup_channel TEXT NOT NULL DEFAULT 'UNKNOWN';

ALTER TABLE users
    ADD CONSTRAINT ck_users_signup_channel CHECK (signup_channel IN ('WEB', 'APP', 'UNKNOWN'));

COMMENT ON COLUMN users.signup_channel IS '가입 경로 (WEB=웹, APP=앱, UNKNOWN=구분 저장 이전 가입)';

--rollback ALTER TABLE users DROP CONSTRAINT ck_users_signup_channel;
--rollback ALTER TABLE users DROP COLUMN signup_channel;
