--liquibase formatted sql

--changeset peakda:20260529-016-create-spot-favorites
CREATE TABLE spot_favorites (
    id              BIGSERIAL   PRIMARY KEY,
    user_id         BIGINT      NOT NULL,
    spot_id         BIGINT      NOT NULL,
    notify_enabled  BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_spot_favorites_user_spot UNIQUE (user_id, spot_id)
);

CREATE INDEX ix_spot_favorites_user_id ON spot_favorites (user_id);
CREATE INDEX ix_spot_favorites_spot_id ON spot_favorites (spot_id);
--rollback DROP TABLE spot_favorites;

--changeset peakda:20260529-016-comment-spot-favorites
COMMENT ON TABLE spot_favorites IS '사용자가 찜한 스팟 (만개 알림 수신 단위)';
COMMENT ON COLUMN spot_favorites.id IS '찜 PK';
COMMENT ON COLUMN spot_favorites.user_id IS '찜한 사용자 id';
COMMENT ON COLUMN spot_favorites.spot_id IS 'spots.id';
COMMENT ON COLUMN spot_favorites.notify_enabled IS '만개 알림 수신 여부 (기본 TRUE)';
COMMENT ON COLUMN spot_favorites.created_at IS '레코드 생성 시각';
COMMENT ON COLUMN spot_favorites.updated_at IS '레코드 최종 수정 시각';
--rollback COMMENT ON TABLE spot_favorites IS NULL;
