--liquibase formatted sql

--changeset peakda:20260514-012-add-weather-mid-forecast-source-region-codes
ALTER TABLE weather_mid_forecasts
    ADD COLUMN source_land_region_code TEXT,
    ADD COLUMN source_temperature_region_code TEXT;

COMMENT ON COLUMN weather_mid_forecasts.region_code IS 'PEAKDA 논리 지역 코드 (MidRegionCode enum name)';
COMMENT ON COLUMN weather_mid_forecasts.source_land_region_code IS '기상청 중기육상예보 원본 지역 코드';
COMMENT ON COLUMN weather_mid_forecasts.source_temperature_region_code IS '기상청 중기기온예보 원본 지역 코드';
--rollback ALTER TABLE weather_mid_forecasts DROP COLUMN source_temperature_region_code, DROP COLUMN source_land_region_code;
