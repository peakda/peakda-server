--liquibase formatted sql

--changeset peakda:20260930-005-create-congestion-attraction-links
CREATE TABLE congestion_attraction_links (
    id                        BIGSERIAL   PRIMARY KEY,
    area_code                 TEXT        NOT NULL,
    sigungu_code              TEXT        NOT NULL,
    tourist_attraction_name   TEXT        NOT NULL,
    attraction_id             BIGINT,
    candidate_attraction_ids  TEXT,
    match_type                TEXT,
    status                    TEXT        NOT NULL,
    created_at                TIMESTAMPTZ NOT NULL,
    updated_at                TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_congestion_attraction_links_key UNIQUE (area_code, sigungu_code, tourist_attraction_name)
);

CREATE INDEX idx_congestion_attraction_links_attraction ON congestion_attraction_links (attraction_id, status);
CREATE INDEX idx_congestion_attraction_links_status ON congestion_attraction_links (status, id);

COMMENT ON TABLE congestion_attraction_links IS '관광지 집중률 관광지명과 명소의 연결. 이름 매칭은 배치에서만 하고 조회는 이 테이블을 쓴다';
COMMENT ON COLUMN congestion_attraction_links.candidate_attraction_ids IS '검토용 후보 명소 id 목록(콤마 구분)';
COMMENT ON COLUMN congestion_attraction_links.match_type IS 'EXACT / REGION_REFORM / CONTAINS / MANUAL';
COMMENT ON COLUMN congestion_attraction_links.status IS 'CONFIRMED / PENDING_REVIEW / REJECTED / UNMATCHED. 화면에는 CONFIRMED 만 노출';

-- 명소 상세에서 연결된 관광지의 향후 7일 예측을 읽는 경로. 기존 유니크 키는 base_date 가 선두라 이 조회에 맞지 않는다.
CREATE INDEX idx_congestions_attraction_base_date
    ON congestions (area_code, sigungu_code, tourist_attraction_name, base_date);
--rollback DROP INDEX idx_congestions_attraction_base_date; DROP TABLE congestion_attraction_links;
