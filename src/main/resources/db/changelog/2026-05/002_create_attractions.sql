--liquibase formatted sql

--changeset peakda:20260513-002-create-attractions
CREATE TABLE attractions (
    id              BIGSERIAL        PRIMARY KEY,
    content_id      TEXT             NOT NULL,
    content_type_id TEXT,
    title           TEXT             NOT NULL,
    addr1           TEXT,
    addr2           TEXT,
    area_code       TEXT,
    sigungu_code    TEXT,
    map_x           DOUBLE PRECISION,
    map_y           DOUBLE PRECISION,
    first_image     TEXT,
    first_image2    TEXT,
    cat1            TEXT,
    cat2            TEXT,
    cat3            TEXT,
    created_time    TEXT,
    modified_time   TEXT,
    visible         BOOLEAN          NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ      NOT NULL,
    updated_at      TIMESTAMPTZ      NOT NULL,
    CONSTRAINT uk_attractions_content_id UNIQUE (content_id)
);

CREATE INDEX idx_attractions_modified_time ON attractions (modified_time);
CREATE INDEX idx_attractions_visible ON attractions (visible);
--rollback DROP TABLE attractions;
