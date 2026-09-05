--liquibase formatted sql

--changeset peakda:20260817-003-create-location-usage-logs
CREATE TABLE location_usage_logs (
    id         BIGSERIAL   PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    channel    TEXT        NOT NULL,
    service    TEXT        NOT NULL,
    used_at    TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_location_usage_logs_user ON location_usage_logs (user_id, used_at DESC);
CREATE INDEX ix_location_usage_logs_used_at ON location_usage_logs (used_at DESC);
--rollback DROP TABLE location_usage_logs;

--changeset peakda:20260817-003-comment-location-usage-logs
COMMENT ON TABLE location_usage_logs IS '위치정보 이용·제공사실 확인자료. 개인위치정보를 이용한 요청 1건당 1행';
COMMENT ON COLUMN location_usage_logs.id IS '위치정보 이용 기록 PK';
COMMENT ON COLUMN location_usage_logs.user_id IS '개인위치정보주체 사용자 id. 이메일 등 식별정보는 users 를 통해 조회한다';
COMMENT ON COLUMN location_usage_logs.channel IS '취득경로. User-Agent 로 판별한 클라이언트 종류';
COMMENT ON COLUMN location_usage_logs.service IS '위치정보를 이용한 제공서비스 종류';
COMMENT ON COLUMN location_usage_logs.used_at IS '위치정보 이용일시';
COMMENT ON COLUMN location_usage_logs.created_at IS '기록 생성 시각';
COMMENT ON COLUMN location_usage_logs.updated_at IS '기록 최종 수정 시각';
--rollback COMMENT ON TABLE location_usage_logs IS NULL;
