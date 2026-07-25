--liquibase formatted sql

--changeset peakda:20260725-009-create-curations
CREATE TABLE curations (
    id                   BIGSERIAL   PRIMARY KEY,
    week_start_date      DATE        NOT NULL,
    week_end_date        DATE        NOT NULL,
    week_label           TEXT        NOT NULL,
    hero_image_url       TEXT,
    title                TEXT        NOT NULL,
    subtitle             TEXT,
    intro                TEXT,
    next_teaser_overline TEXT,
    next_teaser_body     TEXT,
    status               TEXT        NOT NULL,
    published_at         TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL,
    updated_at           TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_curations_week_start_date UNIQUE (week_start_date)
);

CREATE INDEX ix_curations_status_week_start_date ON curations (status, week_start_date DESC);

CREATE TABLE curation_chapters (
    id          BIGSERIAL   PRIMARY KEY,
    curation_id BIGINT      NOT NULL,
    sort_order  INTEGER     NOT NULL,
    layout      TEXT        NOT NULL,
    heading     TEXT        NOT NULL,
    spot_id     BIGINT,
    place_name  TEXT        NOT NULL,
    latitude    DOUBLE PRECISION,
    longitude   DOUBLE PRECISION,
    photo_url   TEXT,
    pull_quote  TEXT,
    lead_text   TEXT,
    body        TEXT        NOT NULL,
    fact_note   TEXT,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_curation_chapters_curation_sort UNIQUE (curation_id, sort_order)
);

CREATE INDEX ix_curation_chapters_curation_id ON curation_chapters (curation_id);

CREATE TABLE curation_recommendations (
    id          BIGSERIAL   PRIMARY KEY,
    curation_id BIGINT      NOT NULL,
    sort_order  INTEGER     NOT NULL,
    title       TEXT        NOT NULL,
    spot_id     BIGINT,
    place_name  TEXT        NOT NULL,
    latitude    DOUBLE PRECISION,
    longitude   DOUBLE PRECISION,
    photo_url   TEXT,
    body        TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_curation_recommendations_curation_sort UNIQUE (curation_id, sort_order)
);

CREATE INDEX ix_curation_recommendations_curation_id ON curation_recommendations (curation_id);
--rollback DROP TABLE curation_recommendations;
--rollback DROP TABLE curation_chapters;
--rollback DROP TABLE curations;

--changeset peakda:20260725-009-comment-curations
COMMENT ON TABLE curations IS 'SCR-026 주차 단위 에디토리얼 큐레이션';
COMMENT ON COLUMN curations.id IS '큐레이션 PK';
COMMENT ON COLUMN curations.week_start_date IS '큐레이션 대상 주차 시작일과 주차 단위 멱등 갱신 키';
COMMENT ON COLUMN curations.week_end_date IS '큐레이션 대상 주차 종료일';
COMMENT ON COLUMN curations.week_label IS '화면 주차 뱃지에 표시할 에디터 작성 문구';
COMMENT ON COLUMN curations.hero_image_url IS '상세 히어로 이미지 URL';
COMMENT ON COLUMN curations.title IS '큐레이션 타이틀';
COMMENT ON COLUMN curations.subtitle IS '큐레이션 부제';
COMMENT ON COLUMN curations.intro IS '최대 세 단락의 도입글';
COMMENT ON COLUMN curations.next_teaser_overline IS '다음 주 예고 오버라인';
COMMENT ON COLUMN curations.next_teaser_body IS '다음 주 예고 본문';
COMMENT ON COLUMN curations.status IS '큐레이션 상태(DRAFT, PUBLISHED)';
COMMENT ON COLUMN curations.published_at IS '최초 또는 최근 발행 전환 시각';
COMMENT ON COLUMN curations.created_at IS '레코드 생성 시각';
COMMENT ON COLUMN curations.updated_at IS '레코드 최종 수정 시각';

COMMENT ON TABLE curation_chapters IS '큐레이션 본문의 정렬된 장소 챕터';
COMMENT ON COLUMN curation_chapters.id IS '큐레이션 챕터 PK';
COMMENT ON COLUMN curation_chapters.curation_id IS '상위 큐레이션 id';
COMMENT ON COLUMN curation_chapters.sort_order IS '챕터 표시 순서(1부터)';
COMMENT ON COLUMN curation_chapters.layout IS '챕터 레이아웃(MAIN, RHYTHM_REVERSE, EDGE_BLEED)';
COMMENT ON COLUMN curation_chapters.heading IS '번호 뒤에 표시할 자유 챕터 레이블';
COMMENT ON COLUMN curation_chapters.spot_id IS '개화 뱃지·거리·상세 링크를 보강할 스팟 id';
COMMENT ON COLUMN curation_chapters.place_name IS '에디터가 입력한 장소명';
COMMENT ON COLUMN curation_chapters.latitude IS '저장된 장소 위도';
COMMENT ON COLUMN curation_chapters.longitude IS '저장된 장소 경도';
COMMENT ON COLUMN curation_chapters.photo_url IS '챕터 사진 URL';
COMMENT ON COLUMN curation_chapters.pull_quote IS '챕터 풀쿼트';
COMMENT ON COLUMN curation_chapters.lead_text IS '레이아웃별 리드 텍스트';
COMMENT ON COLUMN curation_chapters.body IS '최대 세 단락의 챕터 본문';
COMMENT ON COLUMN curation_chapters.fact_note IS '운영기간·입장료·주의사항을 보존하는 단일 자유 텍스트';
COMMENT ON COLUMN curation_chapters.created_at IS '레코드 생성 시각';
COMMENT ON COLUMN curation_chapters.updated_at IS '레코드 최종 수정 시각';

COMMENT ON TABLE curation_recommendations IS '추후 복수 스팟 확장을 고려한 큐레이션 당일치기 추천 카드';
COMMENT ON COLUMN curation_recommendations.id IS '큐레이션 추천 카드 PK';
COMMENT ON COLUMN curation_recommendations.curation_id IS '상위 큐레이션 id';
COMMENT ON COLUMN curation_recommendations.sort_order IS '추천 카드 표시 순서(1부터)';
COMMENT ON COLUMN curation_recommendations.title IS '추천 카드 타이틀';
COMMENT ON COLUMN curation_recommendations.spot_id IS '거리·상세 링크를 보강할 스팟 id';
COMMENT ON COLUMN curation_recommendations.place_name IS '에디터가 입력한 장소명';
COMMENT ON COLUMN curation_recommendations.latitude IS '저장된 장소 위도';
COMMENT ON COLUMN curation_recommendations.longitude IS '저장된 장소 경도';
COMMENT ON COLUMN curation_recommendations.photo_url IS '추천 카드 사진 URL';
COMMENT ON COLUMN curation_recommendations.body IS '추천 카드 설명';
COMMENT ON COLUMN curation_recommendations.created_at IS '레코드 생성 시각';
COMMENT ON COLUMN curation_recommendations.updated_at IS '레코드 최종 수정 시각';
--rollback COMMENT ON TABLE curation_recommendations IS NULL;
--rollback COMMENT ON TABLE curation_chapters IS NULL;
--rollback COMMENT ON TABLE curations IS NULL;
