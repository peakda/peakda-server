--liquibase formatted sql

--changeset peakda:20260513-009-create-gallery-photos
CREATE TABLE gallery_photos (
    id                     BIGSERIAL   PRIMARY KEY,
    tour_api_content_id    TEXT        NOT NULL,
    content_type_code      TEXT,
    title                  TEXT,
    web_image_url          TEXT,
    external_created_at    TEXT,
    external_modified_at   TEXT,
    photography_month      TEXT,
    photography_location   TEXT,
    photographer           TEXT,
    search_keyword         TEXT,
    created_at             TIMESTAMPTZ NOT NULL,
    updated_at             TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_gallery_photos_tour_api_content_id UNIQUE (tour_api_content_id)
);
--rollback DROP TABLE gallery_photos;
