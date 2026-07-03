--liquibase formatted sql

--changeset peakda:20260703-001-create-spot-record-reactions
CREATE TABLE spot_record_reactions (
    id              BIGSERIAL   PRIMARY KEY,
    user_id         BIGINT      NOT NULL,
    spot_record_id  BIGINT      NOT NULL,
    reaction_type   TEXT        NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_spot_record_reactions_user_record_type UNIQUE (user_id, spot_record_id, reaction_type)
);

CREATE INDEX ix_spot_record_reactions_spot_record_id ON spot_record_reactions (spot_record_id);
CREATE INDEX ix_spot_record_reactions_user_id ON spot_record_reactions (user_id);
--rollback DROP TABLE spot_record_reactions;

--changeset peakda:20260703-001-comment-spot-record-reactions
COMMENT ON TABLE spot_record_reactions IS '스팟 기록에 대한 사용자 리액션 (결정 F — V1은 고정 이모지 2종, 댓글은 후속)';
COMMENT ON COLUMN spot_record_reactions.id IS '리액션 PK';
COMMENT ON COLUMN spot_record_reactions.user_id IS '리액션을 남긴 사용자 id';
COMMENT ON COLUMN spot_record_reactions.spot_record_id IS 'spot_records.id';
COMMENT ON COLUMN spot_record_reactions.reaction_type IS '리액션 종류 (HEART, SMILE)';
COMMENT ON COLUMN spot_record_reactions.created_at IS '레코드 생성 시각';
COMMENT ON COLUMN spot_record_reactions.updated_at IS '레코드 최종 수정 시각';
--rollback COMMENT ON TABLE spot_record_reactions IS NULL;
