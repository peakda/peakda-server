--liquibase formatted sql

--changeset peakda:20261021-014-create-launch-alert-subscriptions
CREATE TABLE launch_alert_subscriptions (
    id              BIGSERIAL   PRIMARY KEY,
    platform        TEXT        NOT NULL,
    email           TEXT        NOT NULL,
    consent_version TEXT        NOT NULL,
    consented_at    TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_launch_alert_subscriptions_platform_email UNIQUE (platform, email),
    CONSTRAINT ck_launch_alert_subscriptions_platform CHECK (platform IN ('IOS'))
);

COMMENT ON TABLE launch_alert_subscriptions IS '앱 출시 알림 신청. 랜딩 페이지에서 비로그인으로 받는다. 출시 안내 메일을 보내면 그 신청은 바로 지운다';
COMMENT ON COLUMN launch_alert_subscriptions.id IS '신청 id. 발송은 이 순서로 진행한다';
COMMENT ON COLUMN launch_alert_subscriptions.platform IS '알림 받을 앱 플랫폼 (IOS)';
COMMENT ON COLUMN launch_alert_subscriptions.email IS '알림 받을 이메일. 앞뒤 공백을 지우고 소문자로 저장한다';
COMMENT ON COLUMN launch_alert_subscriptions.consent_version IS '동의한 개인정보 수집·이용 안내 문구 버전';
COMMENT ON COLUMN launch_alert_subscriptions.consented_at IS '개인정보 수집·이용 동의 시각. 다시 신청하면 갱신된다';
COMMENT ON COLUMN launch_alert_subscriptions.created_at IS '최초 신청 시각';
COMMENT ON COLUMN launch_alert_subscriptions.updated_at IS '레코드 최종 수정 시각';
--rollback DROP TABLE launch_alert_subscriptions;

--changeset peakda:20261021-014-create-launch-alert-dispatches
CREATE TABLE launch_alert_dispatches (
    id             BIGSERIAL   PRIMARY KEY,
    platform       TEXT        NOT NULL,
    store_url      TEXT        NOT NULL,
    status         TEXT        NOT NULL,
    requested_by   BIGINT      NOT NULL,
    cursor_id      BIGINT      NOT NULL DEFAULT 0,
    sent_count     INTEGER     NOT NULL DEFAULT 0,
    failed_count   INTEGER     NOT NULL DEFAULT 0,
    completed_at   TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_launch_alert_dispatches_platform CHECK (platform IN ('IOS')),
    CONSTRAINT ck_launch_alert_dispatches_status CHECK (status IN ('DISPATCHING', 'COMPLETED', 'FAILED', 'CANCELED'))
);

CREATE UNIQUE INDEX uk_launch_alert_dispatches_platform_dispatching
    ON launch_alert_dispatches (platform) WHERE status = 'DISPATCHING';

COMMENT ON TABLE launch_alert_dispatches IS '출시 안내 메일 발송 요청. 관리자가 만들면 스케줄러 잡이 묶음 단위로 보낸다';
COMMENT ON COLUMN launch_alert_dispatches.platform IS '보낼 신청의 앱 플랫폼 (IOS)';
COMMENT ON COLUMN launch_alert_dispatches.store_url IS '메일에 넣을 스토어 주소';
COMMENT ON COLUMN launch_alert_dispatches.status IS '발송 상태 (DISPATCHING, COMPLETED, FAILED, CANCELED). 플랫폼마다 DISPATCHING 은 하나뿐이다';
COMMENT ON COLUMN launch_alert_dispatches.requested_by IS '발송을 요청한 관리자 id';
COMMENT ON COLUMN launch_alert_dispatches.cursor_id IS '보내기 시작한 마지막 신청 id. 메일을 보내기 전에 먼저 옮겨, 중간에 끊겨도 같은 사람에게 다시 보내지 않는다. 실패한 신청은 남지만 이 발송에서 다시 보내지 않는다';
COMMENT ON COLUMN launch_alert_dispatches.sent_count IS '보낸 메일 수';
COMMENT ON COLUMN launch_alert_dispatches.failed_count IS '보내지 못한 메일 수. 그 신청은 지우지 않고 남긴다';
COMMENT ON COLUMN launch_alert_dispatches.completed_at IS '발송이 끝나거나 중단·취소된 시각';
COMMENT ON COLUMN launch_alert_dispatches.created_at IS '발송 요청 시각';
COMMENT ON COLUMN launch_alert_dispatches.updated_at IS '레코드 최종 수정 시각';
--rollback DROP TABLE launch_alert_dispatches;
