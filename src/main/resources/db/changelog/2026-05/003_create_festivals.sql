--liquibase formatted sql

--changeset peakda:20260513-003-create-festivals
CREATE TABLE festivals (
    id                BIGSERIAL   PRIMARY KEY,
    fstvl_nm          TEXT        NOT NULL,
    opar              TEXT        NOT NULL,
    fstvl_start_date  TEXT        NOT NULL,
    fstvl_end_date    TEXT,
    mnnst_nm          TEXT,
    auspc_instt_nm    TEXT,
    suprt_instt_nm    TEXT,
    phone_number      TEXT,
    homepage_url      TEXT,
    rdnmadr           TEXT,
    lnmadr            TEXT,
    latitude          DOUBLE PRECISION,
    longitude         DOUBLE PRECISION,
    reference_date    TEXT,
    instt_code        TEXT,
    instt_nm          TEXT,
    created_at        TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_festivals_name_opar_start UNIQUE (fstvl_nm, opar, fstvl_start_date)
);

CREATE INDEX idx_festivals_start_date ON festivals (fstvl_start_date);
--rollback DROP TABLE festivals;
