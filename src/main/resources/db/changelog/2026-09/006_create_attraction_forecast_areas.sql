--liquibase formatted sql

--changeset peakda:20260930-006-create-attraction-forecast-areas
CREATE TABLE attraction_forecast_areas (
    id               BIGSERIAL   PRIMARY KEY,
    attraction_id    BIGINT      NOT NULL,
    grid_x           INTEGER     NOT NULL,
    grid_y           INTEGER     NOT NULL,
    mid_region_code  TEXT,
    created_at       TIMESTAMPTZ NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_attraction_forecast_areas_attraction UNIQUE (attraction_id)
);

COMMENT ON TABLE attraction_forecast_areas IS '명소가 속한 기상청 예보 구역. 단기예보 5km 격자와 중기예보 구역';
COMMENT ON COLUMN attraction_forecast_areas.mid_region_code IS 'weather_mid_forecasts.region_code 와 같은 값 (MidRegionCode 이름)';
--rollback DROP TABLE attraction_forecast_areas;
