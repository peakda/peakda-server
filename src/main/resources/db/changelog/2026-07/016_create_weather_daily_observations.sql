--liquibase formatted sql

--changeset peakda:20260730-016-create-weather-daily-observations
-- 기존 날씨 테이블은 미래 예보만 보관해 과거 실측 기온의 기간 누적이 불가능했다.
-- ASOS 일자료를 지점·관측일 단위로 보관해 GDD 추정에 필요한 실측 기반을 만든다.
CREATE TABLE weather_daily_observations (
    id                  BIGSERIAL        PRIMARY KEY,
    station_id          TEXT             NOT NULL,
    observed_on         DATE             NOT NULL,
    station_name        TEXT,
    avg_temperature     DOUBLE PRECISION,
    min_temperature     DOUBLE PRECISION,
    max_temperature     DOUBLE PRECISION,
    created_at          TIMESTAMPTZ      NOT NULL,
    updated_at          TIMESTAMPTZ      NOT NULL,
    CONSTRAINT uk_weather_daily_observations_station_date UNIQUE (station_id, observed_on)
);

CREATE INDEX ix_weather_daily_observations_observed_on
    ON weather_daily_observations (observed_on);

COMMENT ON COLUMN weather_daily_observations.station_id IS '종관관측 지점번호';
COMMENT ON COLUMN weather_daily_observations.observed_on IS '관측일';
COMMENT ON COLUMN weather_daily_observations.station_name IS '종관관측 지점명';
COMMENT ON COLUMN weather_daily_observations.avg_temperature IS '일평균기온(°C), 관측 결측 시 NULL';
COMMENT ON COLUMN weather_daily_observations.min_temperature IS '일최저기온(°C), 관측 결측 시 NULL';
COMMENT ON COLUMN weather_daily_observations.max_temperature IS '일최고기온(°C), 관측 결측 시 NULL';

--rollback DROP TABLE weather_daily_observations;
