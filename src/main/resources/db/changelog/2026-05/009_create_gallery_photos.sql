--liquibase formatted sql

--changeset peakda:20260513-009-create-gallery-photos
CREATE TABLE gallery_photos (
    id                       BIGSERIAL   PRIMARY KEY,
    gal_content_id           TEXT        NOT NULL,
    gal_content_type_id      TEXT,
    gal_title                TEXT,
    gal_web_image_url        TEXT,
    gal_created_time         TEXT,
    gal_modified_time        TEXT,
    gal_photography_month    TEXT,
    gal_photography_location TEXT,
    gal_photographer         TEXT,
    gal_search_keyword       TEXT,
    created_at               TIMESTAMPTZ NOT NULL,
    updated_at               TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_gallery_photos_content_id UNIQUE (gal_content_id)
);
--rollback DROP TABLE gallery_photos;
