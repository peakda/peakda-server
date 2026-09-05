--liquibase formatted sql

--changeset peakda:20260728-013-create-notices
CREATE TABLE notices (
    id            BIGSERIAL   PRIMARY KEY,
    title         TEXT        NOT NULL,
    body          TEXT        NOT NULL,
    link_type     TEXT        NOT NULL,
    link_url      TEXT,
    target_id     BIGINT,
    status        TEXT        NOT NULL,
    created_by    BIGINT      NOT NULL,
    dispatched_at TIMESTAMPTZ,
    sent_count    INTEGER     NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_notices_status CHECK (status IN ('DRAFT', 'DISPATCHING', 'DISPATCHED', 'CANCELED'))
);

CREATE INDEX ix_notices_status_id ON notices (status, id DESC);

CREATE TABLE notice_dispatches (
    id         BIGSERIAL   PRIMARY KEY,
    notice_id  BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_notice_dispatches_notice_user UNIQUE (notice_id, user_id)
);
--rollback DROP TABLE notice_dispatches;
--rollback DROP TABLE notices;

--changeset peakda:20260728-013-comment-notices
COMMENT ON TABLE notices IS '관리자가 작성하고 비동기 발송하는 공지 원본';
COMMENT ON COLUMN notices.id IS '공지 PK';
COMMENT ON COLUMN notices.title IS '공지 알림 제목';
COMMENT ON COLUMN notices.body IS '공지 알림 본문';
COMMENT ON COLUMN notices.link_type IS '알림 선택 시 이동 방식(INTERNAL, EXTERNAL)';
COMMENT ON COLUMN notices.link_url IS '외부 링크 이동 URL';
COMMENT ON COLUMN notices.target_id IS '내부 화면 이동 대상 id';
COMMENT ON COLUMN notices.status IS '공지 발송 상태(DRAFT, DISPATCHING, DISPATCHED, CANCELED)';
COMMENT ON COLUMN notices.created_by IS '공지를 작성한 관리자 사용자 id';
COMMENT ON COLUMN notices.dispatched_at IS '전체 활성 사용자 팬아웃 완료 시각';
COMMENT ON COLUMN notices.sent_count IS '중복 방지 로그에 기록된 전체 수신자 수';
COMMENT ON COLUMN notices.created_at IS '공지 생성 시각';
COMMENT ON COLUMN notices.updated_at IS '공지 최종 수정 시각';

COMMENT ON TABLE notice_dispatches IS '공지와 사용자 조합별 중복 팬아웃 방지 로그';
COMMENT ON COLUMN notice_dispatches.id IS '공지 발송 로그 PK';
COMMENT ON COLUMN notice_dispatches.notice_id IS '공지 id';
COMMENT ON COLUMN notice_dispatches.user_id IS '수신 사용자 id';
COMMENT ON COLUMN notice_dispatches.created_at IS '공지 발송 로그 생성 시각';
--rollback COMMENT ON TABLE notice_dispatches IS NULL;
--rollback COMMENT ON TABLE notices IS NULL;
