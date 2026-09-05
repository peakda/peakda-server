--liquibase formatted sql

--changeset peakda:20260704-004-create-notifications
CREATE TABLE notifications (
    id           BIGSERIAL   PRIMARY KEY,
    recipient_id BIGINT      NOT NULL,
    type         TEXT        NOT NULL,
    title        TEXT        NOT NULL,
    body         TEXT        NOT NULL,
    link_type    TEXT        NOT NULL,
    link_url     TEXT,
    target_id    BIGINT,
    read_at      TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_notifications_recipient ON notifications (recipient_id, created_at);
CREATE INDEX ix_notifications_recipient_read ON notifications (recipient_id, read_at);
--rollback DROP TABLE notifications;

--changeset peakda:20260704-004-comment-notifications
COMMENT ON TABLE notifications IS '사용자 알림 (P3-1, 4종 TIMING/FOLLOW/REACTION/NOTICE)';
COMMENT ON COLUMN notifications.id IS '알림 PK';
COMMENT ON COLUMN notifications.recipient_id IS '알림 수신자 사용자 id';
COMMENT ON COLUMN notifications.type IS '알림 종류 (TIMING, FOLLOW, REACTION, NOTICE)';
COMMENT ON COLUMN notifications.title IS '알림 제목';
COMMENT ON COLUMN notifications.body IS '알림 본문';
COMMENT ON COLUMN notifications.link_type IS '탭 시 이동 방식 (INTERNAL, EXTERNAL)';
COMMENT ON COLUMN notifications.link_url IS 'EXTERNAL 일 때 이동할 외부 링크';
COMMENT ON COLUMN notifications.target_id IS 'INTERNAL 일 때 이동 대상 id (팔로워 id·기록 id·스팟 id 등)';
COMMENT ON COLUMN notifications.read_at IS '읽은 시각 (NULL 이면 안 읽음)';
COMMENT ON COLUMN notifications.created_at IS '레코드 생성 시각';
COMMENT ON COLUMN notifications.updated_at IS '레코드 최종 수정 시각';
--rollback COMMENT ON TABLE notifications IS NULL;
