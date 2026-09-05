--liquibase formatted sql

--changeset peakda:20260728-011-create-admin-audit-logs
CREATE TABLE admin_audit_logs (
    id          BIGSERIAL   PRIMARY KEY,
    admin_id    BIGINT      NOT NULL,
    action      TEXT        NOT NULL,
    target_type TEXT        NOT NULL,
    target_id   BIGINT      NOT NULL,
    memo        TEXT,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_admin_audit_logs_target ON admin_audit_logs (target_type, target_id, created_at DESC);
CREATE INDEX ix_admin_audit_logs_admin ON admin_audit_logs (admin_id, created_at DESC);
--rollback DROP TABLE admin_audit_logs;

--changeset peakda:20260728-011-comment-admin-audit-logs
COMMENT ON TABLE admin_audit_logs IS '백오피스 관리자 조치 감사 로그';
COMMENT ON COLUMN admin_audit_logs.id IS '관리자 감사 로그 PK';
COMMENT ON COLUMN admin_audit_logs.admin_id IS '조치를 수행한 관리자 사용자 id';
COMMENT ON COLUMN admin_audit_logs.action IS '관리자 조치 종류';
COMMENT ON COLUMN admin_audit_logs.target_type IS '조치 대상 종류';
COMMENT ON COLUMN admin_audit_logs.target_id IS '조치 대상 id';
COMMENT ON COLUMN admin_audit_logs.memo IS '관리자가 남긴 조치 사유 또는 부가 정보';
COMMENT ON COLUMN admin_audit_logs.created_at IS '감사 로그 생성 시각';
COMMENT ON COLUMN admin_audit_logs.updated_at IS '감사 로그 최종 수정 시각';
--rollback COMMENT ON TABLE admin_audit_logs IS NULL;
