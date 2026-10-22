--liquibase formatted sql

--changeset peakda:20261022-015-create-launch-alert-mail-templates
CREATE TABLE launch_alert_mail_templates (
    platform     TEXT        PRIMARY KEY,
    subject      TEXT        NOT NULL,
    headline     TEXT        NOT NULL,
    body         TEXT        NOT NULL,
    button_label TEXT        NOT NULL,
    footer       TEXT        NOT NULL,
    updated_by   BIGINT,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_launch_alert_mail_templates_platform CHECK (platform IN ('IOS'))
);

COMMENT ON TABLE launch_alert_mail_templates IS '출시 안내 메일 문구. 플랫폼마다 한 행이며 백오피스에서 고친다. HTML 레이아웃은 서버에 고정돼 있고 문구만 끼워 넣는다';
COMMENT ON COLUMN launch_alert_mail_templates.platform IS '앱 플랫폼 (IOS)';
COMMENT ON COLUMN launch_alert_mail_templates.subject IS '메일 제목';
COMMENT ON COLUMN launch_alert_mail_templates.headline IS '본문 맨 위 큰 제목';
COMMENT ON COLUMN launch_alert_mail_templates.body IS '본문. 빈 줄로 문단을 나누고 줄바꿈은 그대로 살린다. HTML 은 쓰지 않는다(이스케이프된다)';
COMMENT ON COLUMN launch_alert_mail_templates.button_label IS '스토어로 가는 버튼 문구';
COMMENT ON COLUMN launch_alert_mail_templates.footer IS '하단 안내 문구 (수신 사유·파기 안내)';
COMMENT ON COLUMN launch_alert_mail_templates.updated_by IS '마지막으로 고친 관리자 id. 처음 넣은 기본 문구는 NULL';
COMMENT ON COLUMN launch_alert_mail_templates.created_at IS '레코드 생성 시각';
COMMENT ON COLUMN launch_alert_mail_templates.updated_at IS '문구 최종 수정 시각';

INSERT INTO launch_alert_mail_templates (platform, subject, headline, body, button_label, footer, created_at, updated_at)
VALUES (
    'IOS',
    '피크다 iOS 앱이 출시됐어요',
    '신청해 주신 iOS 앱이 출시됐어요',
    '벚꽃부터 단풍까지, 전국 계절 명소의 지금 상태를 아이폰에서도 확인해 보세요.',
    'App Store에서 받기',
    '이 메일은 피크다 랜딩 페이지에서 iOS 출시 알림을 신청하신 분께 한 번만 보내 드립니다. 보내 드린 뒤 이메일 주소는 바로 파기했으며, 추가로 메일을 보내지 않습니다.',
    now(),
    now()
);
--rollback DROP TABLE launch_alert_mail_templates;
