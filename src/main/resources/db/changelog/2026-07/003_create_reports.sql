--liquibase formatted sql

--changeset peakda:20260703-003-create-reports
CREATE TABLE reports (
    id           BIGSERIAL   PRIMARY KEY,
    reporter_id  BIGINT      NOT NULL,
    target_type  TEXT        NOT NULL,
    target_id    BIGINT      NOT NULL,
    reason       TEXT        NOT NULL,
    detail       TEXT,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_reports_reporter_target UNIQUE (reporter_id, target_type, target_id)
);

CREATE INDEX ix_reports_target ON reports (target_type, target_id);
--rollback DROP TABLE reports;

--changeset peakda:20260703-003-comment-reports
COMMENT ON TABLE reports IS 'UGC 신고 (운영 심사 원천 데이터, 관리자 처리 API 는 별도)';
COMMENT ON COLUMN reports.id IS '신고 PK';
COMMENT ON COLUMN reports.reporter_id IS '신고한 사용자 id';
COMMENT ON COLUMN reports.target_type IS '신고 대상 종류 (V1: SPOT_RECORD)';
COMMENT ON COLUMN reports.target_id IS '신고 대상 id (target_type=SPOT_RECORD 면 spot_records.id)';
COMMENT ON COLUMN reports.reason IS '신고 사유 (SPAM, INAPPROPRIATE, HARASSMENT, ETC)';
COMMENT ON COLUMN reports.detail IS '상세 사유 (선택)';
COMMENT ON COLUMN reports.created_at IS '레코드 생성 시각';
COMMENT ON COLUMN reports.updated_at IS '레코드 최종 수정 시각';
--rollback COMMENT ON TABLE reports IS NULL;
