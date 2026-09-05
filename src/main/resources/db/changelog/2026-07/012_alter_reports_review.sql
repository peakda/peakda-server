--liquibase formatted sql

--changeset peakda:20260728-012-alter-reports-review
ALTER TABLE reports
    ADD COLUMN status      TEXT NOT NULL DEFAULT 'PENDING',
    ADD COLUMN reviewed_by BIGINT,
    ADD COLUMN reviewed_at TIMESTAMPTZ,
    ADD COLUMN review_memo TEXT;

ALTER TABLE reports
    ADD CONSTRAINT ck_reports_status CHECK (status IN ('PENDING', 'RESOLVED', 'DISMISSED'));

CREATE INDEX ix_reports_status_created_at ON reports (status, created_at DESC);

COMMENT ON COLUMN reports.status IS '신고 심사 상태 (PENDING, RESOLVED, DISMISSED)';
COMMENT ON COLUMN reports.reviewed_by IS '심사한 관리자 사용자 id';
COMMENT ON COLUMN reports.reviewed_at IS '심사 완료 시각';
COMMENT ON COLUMN reports.review_memo IS '관리자가 남긴 심사 메모';

--rollback DROP INDEX ix_reports_status_created_at;
--rollback ALTER TABLE reports DROP CONSTRAINT ck_reports_status;
--rollback ALTER TABLE reports DROP COLUMN review_memo, DROP COLUMN reviewed_at, DROP COLUMN reviewed_by, DROP COLUMN status;
