--liquibase formatted sql

--changeset peakda:20260513-005-create-weather-short-forecasts
CREATE TABLE weather_short_forecasts (
    id          BIGSERIAL   PRIMARY KEY,
    nx          INTEGER     NOT NULL,
    ny          INTEGER     NOT NULL,
    base_date   TEXT        NOT NULL,
    base_time   TEXT        NOT NULL,
    fcst_date   TEXT        NOT NULL,
    fcst_time   TEXT        NOT NULL,
    category    TEXT        NOT NULL,
    fcst_value  TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_weather_short_forecasts_grid_fcst UNIQUE (nx, ny, fcst_date, fcst_time, category)
);

CREATE INDEX idx_weather_short_forecasts_fcst ON weather_short_forecasts (fcst_date, fcst_time);
--rollback DROP TABLE weather_short_forecasts;
