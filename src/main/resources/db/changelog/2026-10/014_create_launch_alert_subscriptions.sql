--liquibase formatted sql

--changeset peakda:20261021-014-create-launch-alert-subscriptions
CREATE TABLE launch_alert_subscriptions (
    id              BIGSERIAL   PRIMARY KEY,
    platform        TEXT        NOT NULL,
    email           TEXT        NOT NULL,
    consent_version TEXT        NOT NULL,
    consented_at    TIMESTAMPTZ NOT NULL,
    exported_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_launch_alert_subscriptions_platform_email UNIQUE (platform, email),
    CONSTRAINT ck_launch_alert_subscriptions_platform CHECK (platform IN ('IOS'))
);

COMMENT ON TABLE launch_alert_subscriptions IS '앱 출시 알림 신청. 랜딩 페이지에서 비로그인으로 받는다. 출시 안내 메일을 보낸 뒤 관리자가 파기한다';
COMMENT ON COLUMN launch_alert_subscriptions.id IS '신청 id';
COMMENT ON COLUMN launch_alert_subscriptions.platform IS '알림 받을 앱 플랫폼 (IOS)';
COMMENT ON COLUMN launch_alert_subscriptions.email IS '알림 받을 이메일. 앞뒤 공백을 지우고 소문자로 저장한다';
COMMENT ON COLUMN launch_alert_subscriptions.consent_version IS '동의한 개인정보 수집·이용 안내 문구 버전';
COMMENT ON COLUMN launch_alert_subscriptions.consented_at IS '개인정보 수집·이용 동의 시각. 다시 신청하면 갱신된다';
COMMENT ON COLUMN launch_alert_subscriptions.exported_at IS '관리자가 발송 대상으로 내려받은 시각. 파기는 이 값이 있는 신청만 지운다';
COMMENT ON COLUMN launch_alert_subscriptions.created_at IS '최초 신청 시각';
COMMENT ON COLUMN launch_alert_subscriptions.updated_at IS '레코드 최종 수정 시각';
--rollback DROP TABLE launch_alert_subscriptions;
