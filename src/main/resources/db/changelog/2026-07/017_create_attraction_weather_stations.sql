--liquibase formatted sql

--changeset peakda:20260730-017-create-attraction-weather-stations
-- 명소마다 최근접 ASOS 지점을 보관해 GDD가 전국 기본 지점 하나에 묶이지 않도록 한다.
CREATE TABLE attraction_weather_stations (
    id                  BIGSERIAL        PRIMARY KEY,
    attraction_id       BIGINT           NOT NULL,
    station_id          TEXT             NOT NULL,
    distance_meters     DOUBLE PRECISION NOT NULL,
    created_at          TIMESTAMPTZ      NOT NULL,
    updated_at          TIMESTAMPTZ      NOT NULL,
    CONSTRAINT uk_attraction_weather_stations_attraction UNIQUE (attraction_id)
);

COMMENT ON COLUMN attraction_weather_stations.id IS '명소별 관측지점 매핑 PK';
COMMENT ON COLUMN attraction_weather_stations.attraction_id IS 'attractions.id (FK 제약 없이 application 무결성)';
COMMENT ON COLUMN attraction_weather_stations.station_id IS '최근접 종관관측 지점번호';
COMMENT ON COLUMN attraction_weather_stations.distance_meters IS '명소와 관측지점 사이의 구면 거리(m)';
COMMENT ON COLUMN attraction_weather_stations.created_at IS '레코드 생성 시각';
COMMENT ON COLUMN attraction_weather_stations.updated_at IS '레코드 최종 수정 시각';

--rollback DROP TABLE attraction_weather_stations;
