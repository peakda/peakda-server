--liquibase formatted sql

--changeset peakda:20260606-003-create-seasonal-bloom-estimates
CREATE TABLE seasonal_bloom_estimates (
    id                  BIGSERIAL        PRIMARY KEY,
    attraction_id       BIGINT           NOT NULL,
    bloom_category      TEXT             NOT NULL,
    base_date           DATE             NOT NULL,
    status              TEXT             NOT NULL,
    confidence          DOUBLE PRECISION NOT NULL,
    chosen_estimator    TEXT             NOT NULL,
    peak_start_date     DATE,
    peak_end_date       DATE,
    peak_duration_days  INTEGER,
    gdd_ratio           DOUBLE PRECISION,
    evidence            TEXT,
    created_at          TIMESTAMPTZ      NOT NULL,
    updated_at          TIMESTAMPTZ      NOT NULL,
    CONSTRAINT uk_seasonal_bloom_estimates_attraction_category_date UNIQUE (attraction_id, bloom_category, base_date),
    CONSTRAINT ck_seasonal_bloom_estimates_status CHECK (status IN ('PREPARING', 'STARTED', 'PEAK', 'ENDED')),
    CONSTRAINT ck_seasonal_bloom_estimates_estimator CHECK (chosen_estimator IN ('GDD', 'FESTIVAL', 'CALENDAR', 'USER_RECORD')),
    CONSTRAINT ck_seasonal_bloom_estimates_category CHECK (bloom_category IN (
        'PLUM', 'FORSYTHIA', 'AZALEA_KR', 'CHERRY', 'CANOLA', 'AZALEA',
        'HYDRANGEA', 'LOTUS', 'COSMOS', 'PINK_MUHLY', 'SILVERGRASS', 'MAPLE', 'CAMELLIA'
    ))
);

CREATE INDEX ix_seasonal_bloom_estimates_base_date     ON seasonal_bloom_estimates (base_date);
CREATE INDEX ix_seasonal_bloom_estimates_status        ON seasonal_bloom_estimates (status);
CREATE INDEX ix_seasonal_bloom_estimates_category_date ON seasonal_bloom_estimates (bloom_category, base_date);
--rollback DROP TABLE seasonal_bloom_estimates;

--changeset peakda:20260606-003-comment-seasonal-bloom-estimates
COMMENT ON TABLE seasonal_bloom_estimates IS 'Q1 산출물 — 명소×카테고리의 산출일(base_date) 시점 개화 상태 (일별 누적 아카이브)';
COMMENT ON COLUMN seasonal_bloom_estimates.id IS '추정 PK';
COMMENT ON COLUMN seasonal_bloom_estimates.attraction_id IS 'attractions.id (FK 제약 없이 application 무결성)';
COMMENT ON COLUMN seasonal_bloom_estimates.bloom_category IS '꽃·계절 카테고리 (BloomCategory enum)';
COMMENT ON COLUMN seasonal_bloom_estimates.base_date IS '산출 기준일 (일별 1행)';
COMMENT ON COLUMN seasonal_bloom_estimates.status IS '개화 상태 (PREPARING/STARTED/PEAK/ENDED)';
COMMENT ON COLUMN seasonal_bloom_estimates.confidence IS '융합 신뢰도 (0~1)';
COMMENT ON COLUMN seasonal_bloom_estimates.chosen_estimator IS '채택된 추정기 (GDD/FESTIVAL/CALENDAR/USER_RECORD)';
COMMENT ON COLUMN seasonal_bloom_estimates.peak_start_date IS '예측 절정 시작일 (올해 만개 시기)';
COMMENT ON COLUMN seasonal_bloom_estimates.peak_end_date IS '예측 절정 종료일';
COMMENT ON COLUMN seasonal_bloom_estimates.peak_duration_days IS '예측 절정 지속일수 (만개지속일)';
COMMENT ON COLUMN seasonal_bloom_estimates.gdd_ratio IS 'GDD 추정기 정규화 비율 (있을 때만)';
COMMENT ON COLUMN seasonal_bloom_estimates.evidence IS '채택 근거 (매칭 축제 id 등 디버깅용 짧은 문자열)';
COMMENT ON COLUMN seasonal_bloom_estimates.created_at IS '레코드 생성 시각';
COMMENT ON COLUMN seasonal_bloom_estimates.updated_at IS '레코드 최종 수정 시각';
--rollback COMMENT ON TABLE seasonal_bloom_estimates IS NULL;
