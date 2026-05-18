--liquibase formatted sql

--changeset peakda:20260518-013-add-users-role
ALTER TABLE users
    ADD COLUMN role TEXT NOT NULL DEFAULT 'USER';

ALTER TABLE users
    ADD CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN'));

CREATE INDEX idx_users_role ON users (role);

COMMENT ON COLUMN users.role IS '사용자 권한 (USER=일반, ADMIN=운영용 actuator 등 접근)';

--rollback DROP INDEX IF EXISTS idx_users_role;
--rollback ALTER TABLE users DROP CONSTRAINT ck_users_role;
--rollback ALTER TABLE users DROP COLUMN role;
