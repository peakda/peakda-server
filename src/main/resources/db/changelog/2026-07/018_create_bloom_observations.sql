--liquibase formatted sql

--changeset peakda:20260730-018-create-bloom-observations
-- 기상청이 현재 시즌만 제공하므로 수종·장소·연도별 계절관측을 누적 보관한다.
CREATE TABLE bloom_observations (
    id                  BIGSERIAL   PRIMARY KEY,
    tree_type           TEXT        NOT NULL,
    obs_place           TEXT        NOT NULL,
    obs_year            INTEGER     NOT NULL,
    obs_place_detail    TEXT,
    flower_status       TEXT,
    budding_on          DATE,
    flowering_on        DATE,
    full_bloom_on       DATE,
    source_modified_at  TEXT,
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_bloom_observations_tree_place_year UNIQUE (tree_type, obs_place, obs_year)
);

COMMENT ON COLUMN bloom_observations.id IS '계절관측 PK';
COMMENT ON COLUMN bloom_observations.tree_type IS '기상청 관측 수종명';
COMMENT ON COLUMN bloom_observations.obs_place IS '기상청 관측 장소명';
COMMENT ON COLUMN bloom_observations.obs_year IS '관측 연도';
COMMENT ON COLUMN bloom_observations.obs_place_detail IS '관측 장소 상세 위치';
COMMENT ON COLUMN bloom_observations.flower_status IS '기상청 개화 상태 코드';
COMMENT ON COLUMN bloom_observations.budding_on IS '개화 전 관측일';
COMMENT ON COLUMN bloom_observations.flowering_on IS '개화일';
COMMENT ON COLUMN bloom_observations.full_bloom_on IS '만발일';
COMMENT ON COLUMN bloom_observations.source_modified_at IS '기상청 원본 최종 수정 시각';
COMMENT ON COLUMN bloom_observations.created_at IS '레코드 생성 시각';
COMMENT ON COLUMN bloom_observations.updated_at IS '레코드 최종 수정 시각';

--rollback DROP TABLE bloom_observations;
