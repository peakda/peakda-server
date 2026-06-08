--liquibase formatted sql

--changeset peakda:20260608-004-create-follows
CREATE TABLE follows (
    id           BIGSERIAL   PRIMARY KEY,
    follower_id  BIGINT      NOT NULL,
    following_id BIGINT      NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_follows_follower_following UNIQUE (follower_id, following_id)
);

CREATE INDEX ix_follows_follower_id ON follows (follower_id);
CREATE INDEX ix_follows_following_id ON follows (following_id);
--rollback DROP TABLE follows;

--changeset peakda:20260608-004-comment-follows
COMMENT ON TABLE follows IS '사용자 간 팔로우 관계 (follower 가 following 을 팔로우)';
COMMENT ON COLUMN follows.id IS '팔로우 PK';
COMMENT ON COLUMN follows.follower_id IS '팔로우를 하는 주체 사용자 id';
COMMENT ON COLUMN follows.following_id IS '팔로우 대상 사용자 id';
COMMENT ON COLUMN follows.created_at IS '레코드 생성 시각';
COMMENT ON COLUMN follows.updated_at IS '레코드 최종 수정 시각';
--rollback COMMENT ON TABLE follows IS NULL;
