--liquibase formatted sql

--changeset peakda:20260513-005-create-weather-short-forecasts
CREATE TABLE weather_short_forecasts (
    id                  BIGSERIAL   PRIMARY KEY,
    grid_x              INTEGER     NOT NULL,
    grid_y              INTEGER     NOT NULL,
    announce_date       TEXT        NOT NULL,
    announce_time       TEXT        NOT NULL,
    forecast_date       TEXT        NOT NULL,
    forecast_time       TEXT        NOT NULL,
    forecast_category   TEXT        NOT NULL,
    forecast_value      TEXT        NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_weather_short_forecasts_grid_forecast UNIQUE (grid_x, grid_y, forecast_date, forecast_time, forecast_category)
);

CREATE INDEX idx_weather_short_forecasts_forecast ON weather_short_forecasts (forecast_date, forecast_time);
--rollback DROP TABLE weather_short_forecasts;
