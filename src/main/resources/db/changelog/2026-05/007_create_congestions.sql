--liquibase formatted sql

--changeset peakda:20260513-007-create-congestions
CREATE TABLE congestions (
    id                       BIGSERIAL   PRIMARY KEY,
    base_date                TEXT        NOT NULL,
    tourist_attraction_code  TEXT        NOT NULL,
    tourist_attraction_name  TEXT,
    area_code                TEXT,
    sigungu_code             TEXT,
    congestion_rate          TEXT,
    created_at               TIMESTAMPTZ NOT NULL,
    updated_at               TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_congestions_date_attraction UNIQUE (base_date, tourist_attraction_code)
);

CREATE INDEX idx_congestions_base_date ON congestions (base_date DESC);
--rollback DROP TABLE congestions;
