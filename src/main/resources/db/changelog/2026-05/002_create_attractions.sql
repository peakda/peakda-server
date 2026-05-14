--liquibase formatted sql

--changeset peakda:20260513-002-create-attractions
CREATE TABLE attractions (
    id                    BIGSERIAL        PRIMARY KEY,
    tour_api_content_id   TEXT             NOT NULL,
    content_type_code     TEXT,
    title                 TEXT             NOT NULL,
    address_main          TEXT,
    address_detail        TEXT,
    area_code             TEXT,
    sigungu_code          TEXT,
    longitude             DOUBLE PRECISION,
    latitude              DOUBLE PRECISION,
    primary_image_url     TEXT,
    thumbnail_image_url   TEXT,
    category_major        TEXT,
    category_medium       TEXT,
    category_minor        TEXT,
    external_created_at   TEXT,
    external_modified_at  TEXT,
    visible               BOOLEAN          NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ      NOT NULL,
    updated_at            TIMESTAMPTZ      NOT NULL,
    CONSTRAINT uk_attractions_tour_api_content_id UNIQUE (tour_api_content_id)
);

CREATE INDEX idx_attractions_external_modified_at ON attractions (external_modified_at);
CREATE INDEX idx_attractions_visible ON attractions (visible);
--rollback DROP TABLE attractions;
