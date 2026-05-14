--liquibase formatted sql

--changeset peakda:20260513-006-create-walking-routes
CREATE TABLE walking_routes (
    id                  BIGSERIAL   PRIMARY KEY,
    durunubi_route_id   TEXT        NOT NULL,
    route_name          TEXT,
    region_division     TEXT,
    theme_name          TEXT,
    city_county         TEXT,
    distance            TEXT,
    required_time       TEXT,
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_walking_routes_durunubi_route_id UNIQUE (durunubi_route_id)
);
--rollback DROP TABLE walking_routes;

--changeset peakda:20260513-006-create-walking-courses-table
CREATE TABLE walking_courses (
    id                    BIGSERIAL   PRIMARY KEY,
    durunubi_course_id    TEXT        NOT NULL,
    durunubi_route_id     TEXT,
    name                  TEXT,
    distance              TEXT,
    total_required_time   TEXT,
    difficulty_level      TEXT,
    city_county           TEXT,
    region_division       TEXT,
    created_at            TIMESTAMPTZ NOT NULL,
    updated_at            TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_walking_courses_durunubi_course_id UNIQUE (durunubi_course_id)
);

CREATE INDEX idx_walking_courses_durunubi_route_id ON walking_courses (durunubi_route_id);
--rollback DROP TABLE walking_courses;
