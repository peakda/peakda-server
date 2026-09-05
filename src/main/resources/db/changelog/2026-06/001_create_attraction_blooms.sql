--liquibase formatted sql

--changeset peakda:20260606-001-create-attraction-blooms
CREATE TABLE attraction_blooms (
    id              BIGSERIAL        PRIMARY KEY,
    attraction_id   BIGINT           NOT NULL,
    bloom_category  TEXT             NOT NULL,
    source          TEXT             NOT NULL,
    confidence      DOUBLE PRECISION NOT NULL,
    evidence        TEXT,
    created_at      TIMESTAMPTZ      NOT NULL,
    updated_at      TIMESTAMPTZ      NOT NULL,
    CONSTRAINT uk_attraction_blooms_attraction_category_source UNIQUE (attraction_id, bloom_category, source),
    CONSTRAINT ck_attraction_blooms_source CHECK (source IN ('KEYWORD', 'FESTIVAL', 'MANUAL', 'EXIF_BOOST')),
    CONSTRAINT ck_attraction_blooms_category CHECK (bloom_category IN (
        'PLUM', 'FORSYTHIA', 'AZALEA_KR', 'CHERRY', 'CANOLA', 'AZALEA',
        'HYDRANGEA', 'LOTUS', 'COSMOS', 'PINK_MUHLY', 'SILVERGRASS', 'MAPLE', 'CAMELLIA'
    ))
);

CREATE INDEX ix_attraction_blooms_category   ON attraction_blooms (bloom_category);
CREATE INDEX ix_attraction_blooms_attraction ON attraction_blooms (attraction_id);
--rollback DROP TABLE attraction_blooms;

--changeset peakda:20260606-001-comment-attraction-blooms
COMMENT ON TABLE attraction_blooms IS '명소 ↔ 꽃·계절 카테고리 매핑 (자동 태깅 출처/신뢰도 단위)';
COMMENT ON COLUMN attraction_blooms.id IS '매핑 PK';
COMMENT ON COLUMN attraction_blooms.attraction_id IS 'attractions.id (FK 제약 없이 application 무결성)';
COMMENT ON COLUMN attraction_blooms.bloom_category IS '꽃·계절 카테고리 (BloomCategory enum)';
COMMENT ON COLUMN attraction_blooms.source IS '태깅 출처 (KEYWORD/FESTIVAL/MANUAL/EXIF_BOOST)';
COMMENT ON COLUMN attraction_blooms.confidence IS '태깅 신뢰도 (0~1)';
COMMENT ON COLUMN attraction_blooms.evidence IS '매칭 근거 (키워드·축제명 등 디버깅용 짧은 문자열)';
COMMENT ON COLUMN attraction_blooms.created_at IS '레코드 생성 시각';
COMMENT ON COLUMN attraction_blooms.updated_at IS '레코드 최종 수정 시각';
--rollback COMMENT ON TABLE attraction_blooms IS NULL;
