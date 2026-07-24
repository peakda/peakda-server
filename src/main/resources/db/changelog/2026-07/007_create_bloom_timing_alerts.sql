--liquibase formatted sql

--changeset peakda:20260724-007-create-bloom-timing-alerts
CREATE TABLE bloom_timing_alerts (
    id              BIGSERIAL   PRIMARY KEY,
    user_id         BIGINT      NOT NULL,
    spot_id         BIGINT      NOT NULL,
    bloom_category  TEXT        NOT NULL,
    peak_year       INTEGER     NOT NULL,
    peak_start_date DATE        NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_bloom_timing_alerts_user_spot_category_year UNIQUE (user_id, spot_id, bloom_category, peak_year)
);

CREATE INDEX ix_bloom_timing_alerts_user_id ON bloom_timing_alerts (user_id);
--rollback DROP TABLE bloom_timing_alerts;

--changeset peakda:20260724-007-comment-bloom-timing-alerts
COMMENT ON TABLE bloom_timing_alerts IS '만개 임박 알림 발송 로그 (P3-3, user×spot×카테고리×만개연도 1회 발송 보장)';
COMMENT ON COLUMN bloom_timing_alerts.id IS '만개 알림 로그 PK';
COMMENT ON COLUMN bloom_timing_alerts.user_id IS '알림 수신자(찜 소유자) 사용자 id';
COMMENT ON COLUMN bloom_timing_alerts.spot_id IS '찜한 스팟 id';
COMMENT ON COLUMN bloom_timing_alerts.bloom_category IS '만개 임박 꽃 카테고리';
COMMENT ON COLUMN bloom_timing_alerts.peak_year IS '만개 시작 예상 연도 (중복 방지 키)';
COMMENT ON COLUMN bloom_timing_alerts.peak_start_date IS '알림 시점의 만개 시작 예상일';
COMMENT ON COLUMN bloom_timing_alerts.created_at IS '레코드 생성 시각(=알림 발송 시각)';
COMMENT ON COLUMN bloom_timing_alerts.updated_at IS '레코드 최종 수정 시각';
--rollback COMMENT ON TABLE bloom_timing_alerts IS NULL;
