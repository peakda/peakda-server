--liquibase formatted sql

--changeset peakda:20260513-003-create-festivals
CREATE TABLE festivals (
    id                            BIGSERIAL   PRIMARY KEY,
    name                          TEXT        NOT NULL,
    venue                         TEXT        NOT NULL,
    start_date                    TEXT        NOT NULL,
    end_date                      TEXT,
    host_organization             TEXT,
    organizing_institution        TEXT,
    supporting_institution        TEXT,
    phone_number                  TEXT,
    homepage_url                  TEXT,
    road_address                  TEXT,
    land_lot_address              TEXT,
    latitude                      DOUBLE PRECISION,
    longitude                     DOUBLE PRECISION,
    reference_date                TEXT,
    provider_institution_code     TEXT,
    provider_institution_name     TEXT,
    created_at                    TIMESTAMPTZ NOT NULL,
    updated_at                    TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_festivals_name_venue_start UNIQUE (name, venue, start_date)
);

CREATE INDEX idx_festivals_start_date ON festivals (start_date);
--rollback DROP TABLE festivals;
