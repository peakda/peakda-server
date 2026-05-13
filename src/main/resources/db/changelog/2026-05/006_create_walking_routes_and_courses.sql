--liquibase formatted sql

--changeset peakda:20260513-006-create-walking-routes
CREATE TABLE walking_routes (
    id            BIGSERIAL   PRIMARY KEY,
    route_idx     TEXT        NOT NULL,
    route_name    TEXT,
    brd_div       TEXT,
    theme_nm      TEXT,
    sigun         TEXT,
    distance      TEXT,
    required_time TEXT,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_walking_routes_route_idx UNIQUE (route_idx)
);
--rollback DROP TABLE walking_routes;

--changeset peakda:20260513-006-create-walking-courses-table
CREATE TABLE walking_courses (
    id                  BIGSERIAL   PRIMARY KEY,
    crs_idx             TEXT        NOT NULL,
    route_idx           TEXT,
    crs_kor_nm          TEXT,
    crs_dstnc           TEXT,
    crs_totl_rqrm_hour  TEXT,
    crs_level           TEXT,
    sigun               TEXT,
    brd_div             TEXT,
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_walking_courses_crs_idx UNIQUE (crs_idx)
);

CREATE INDEX idx_walking_courses_route_idx ON walking_courses (route_idx);
--rollback DROP TABLE walking_courses;
