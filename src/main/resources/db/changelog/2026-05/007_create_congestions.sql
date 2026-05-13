--liquibase formatted sql

--changeset peakda:20260513-007-create-congestions
CREATE TABLE congestions (
    id          BIGSERIAL   PRIMARY KEY,
    base_ymd    TEXT        NOT NULL,
    t_ats_cd    TEXT        NOT NULL,
    t_ats_nm    TEXT,
    area_cd     TEXT,
    signgu_cd   TEXT,
    cnctr_rate  TEXT,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_congestions_ymd_tats UNIQUE (base_ymd, t_ats_cd)
);

CREATE INDEX idx_congestions_base_ymd ON congestions (base_ymd DESC);
--rollback DROP TABLE congestions;
