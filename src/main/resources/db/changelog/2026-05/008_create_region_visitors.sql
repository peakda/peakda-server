--liquibase formatted sql

--changeset peakda:20260513-008-create-region-visitors
CREATE TABLE region_visitors (
    id              BIGSERIAL   PRIMARY KEY,
    base_ymd        TEXT        NOT NULL,
    area_cd         TEXT        NOT NULL,
    area_nm         TEXT,
    tou_div_cd      TEXT        NOT NULL,
    tou_div_nm      TEXT,
    num             BIGINT,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_region_visitors_ymd_area_div UNIQUE (base_ymd, area_cd, tou_div_cd)
);

CREATE INDEX idx_region_visitors_base_ymd ON region_visitors (base_ymd DESC);
--rollback DROP TABLE region_visitors;
