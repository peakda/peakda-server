--liquibase formatted sql

--changeset peakda:20261002-008-create-attraction-operating-infos
CREATE TABLE attraction_operating_infos (
    id                  BIGSERIAL   PRIMARY KEY,
    attraction_id       BIGINT      NOT NULL,
    operating_hours     TEXT,
    closed_days         TEXT,
    admission_fee       TEXT,
    parking             TEXT,
    source_modified_at  TEXT,
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_attraction_operating_infos_attraction UNIQUE (attraction_id)
);

COMMENT ON TABLE attraction_operating_infos IS '스팟 상세 운영 정보. 한국관광공사 소개정보(detailIntro2)·반복정보(detailInfo2)에서 받는다';
COMMENT ON COLUMN attraction_operating_infos.operating_hours IS '이용시간 (detailIntro2 usetime)';
COMMENT ON COLUMN attraction_operating_infos.closed_days IS '쉬는날 (detailIntro2 restdate)';
COMMENT ON COLUMN attraction_operating_infos.admission_fee IS '입장료 (detailInfo2 반복정보 중 입장료·이용요금·관람료)';
COMMENT ON COLUMN attraction_operating_infos.parking IS '주차시설 (detailIntro2 parking)';
COMMENT ON COLUMN attraction_operating_infos.source_modified_at IS '받을 때의 attractions.external_modified_at. 값이 달라지면 다시 받는다';
--rollback DROP TABLE attraction_operating_infos;
