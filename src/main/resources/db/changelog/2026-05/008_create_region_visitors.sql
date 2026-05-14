--liquibase formatted sql

--changeset peakda:20260513-008-create-region-visitors
CREATE TABLE region_visitors (
    id                  BIGSERIAL   PRIMARY KEY,
    base_date           TEXT        NOT NULL,
    area_code           TEXT        NOT NULL,
    area_name           TEXT,
    tourist_type_code   TEXT        NOT NULL,
    tourist_type_name   TEXT,
    visitor_count       BIGINT,
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_region_visitors_date_area_type UNIQUE (base_date, area_code, tourist_type_code)
);

CREATE INDEX idx_region_visitors_base_date ON region_visitors (base_date DESC);
--rollback DROP TABLE region_visitors;
