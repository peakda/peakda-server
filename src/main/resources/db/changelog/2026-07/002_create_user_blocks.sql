--liquibase formatted sql

--changeset peakda:20260703-002-create-user-blocks
CREATE TABLE user_blocks (
    id          BIGSERIAL   PRIMARY KEY,
    blocker_id  BIGINT      NOT NULL,
    blocked_id  BIGINT      NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_user_blocks_blocker_blocked UNIQUE (blocker_id, blocked_id)
);

CREATE INDEX ix_user_blocks_blocker_id ON user_blocks (blocker_id);
CREATE INDEX ix_user_blocks_blocked_id ON user_blocks (blocked_id);
--rollback DROP TABLE user_blocks;

--changeset peakda:20260703-002-comment-user-blocks
COMMENT ON TABLE user_blocks IS '사용자 간 차단 관계 (blocker 가 blocked 를 차단)';
COMMENT ON COLUMN user_blocks.id IS '차단 PK';
COMMENT ON COLUMN user_blocks.blocker_id IS '차단을 하는 주체 사용자 id';
COMMENT ON COLUMN user_blocks.blocked_id IS '차단 대상 사용자 id';
COMMENT ON COLUMN user_blocks.created_at IS '레코드 생성 시각';
COMMENT ON COLUMN user_blocks.updated_at IS '레코드 최종 수정 시각';
--rollback COMMENT ON TABLE user_blocks IS NULL;
