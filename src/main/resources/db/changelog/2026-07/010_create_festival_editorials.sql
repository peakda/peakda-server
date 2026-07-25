--liquibase formatted sql

--changeset peakda:20260725-010-create-festival-editorials
CREATE TABLE festival_editorials (
    id                   BIGSERIAL   PRIMARY KEY,
    festival_id          BIGINT      NOT NULL,
    hook                 TEXT,
    period_note          TEXT,
    place_note           TEXT,
    admission_fee        TEXT,
    admission_fee_note   TEXT,
    operating_hours      TEXT,
    operating_hours_note TEXT,
    caution              TEXT,
    caution_note         TEXT,
    directions_transit   TEXT,
    directions_car       TEXT,
    hero_image_url       TEXT,
    status               TEXT        NOT NULL,
    published_at         TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL,
    updated_at           TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_festival_editorials_festival_id UNIQUE (festival_id)
);

CREATE TABLE festival_highlights (
    id                    BIGSERIAL   PRIMARY KEY,
    festival_editorial_id BIGINT      NOT NULL,
    sort_order            INTEGER     NOT NULL,
    title                 TEXT        NOT NULL,
    body                  TEXT        NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL,
    updated_at            TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_festival_highlights_editorial_sort UNIQUE (festival_editorial_id, sort_order)
);

CREATE INDEX ix_festival_highlights_editorial_id ON festival_highlights (festival_editorial_id);
--rollback DROP TABLE festival_highlights;
--rollback DROP TABLE festival_editorials;

--changeset peakda:20260725-010-comment-festival-editorials
COMMENT ON TABLE festival_editorials IS 'SCR-027 축제별 운영·에디토리얼 정보';
COMMENT ON COLUMN festival_editorials.id IS '축제 에디토리얼 PK';
COMMENT ON COLUMN festival_editorials.festival_id IS '원천 축제 id와 축제 단위 멱등 갱신 키';
COMMENT ON COLUMN festival_editorials.hook IS '축제 상세 에디토리얼 훅';
COMMENT ON COLUMN festival_editorials.period_note IS '축제 기간 블록 서브텍스트';
COMMENT ON COLUMN festival_editorials.place_note IS '장소 블록 서브텍스트';
COMMENT ON COLUMN festival_editorials.admission_fee IS '입장료 블록 값';
COMMENT ON COLUMN festival_editorials.admission_fee_note IS '입장료 블록 서브텍스트';
COMMENT ON COLUMN festival_editorials.operating_hours IS '운영 시간 블록 값';
COMMENT ON COLUMN festival_editorials.operating_hours_note IS '운영 시간 블록 서브텍스트';
COMMENT ON COLUMN festival_editorials.caution IS '주의사항 블록 값';
COMMENT ON COLUMN festival_editorials.caution_note IS '주의사항 블록 서브텍스트';
COMMENT ON COLUMN festival_editorials.directions_transit IS '개행으로 구분한 대중교통 안내';
COMMENT ON COLUMN festival_editorials.directions_car IS '개행으로 구분한 자가 차량 안내';
COMMENT ON COLUMN festival_editorials.hero_image_url IS '축제 상세 히어로 이미지 URL';
COMMENT ON COLUMN festival_editorials.status IS '축제 에디토리얼 상태(DRAFT, PUBLISHED)';
COMMENT ON COLUMN festival_editorials.published_at IS '최초 또는 최근 발행 전환 시각';
COMMENT ON COLUMN festival_editorials.created_at IS '레코드 생성 시각';
COMMENT ON COLUMN festival_editorials.updated_at IS '레코드 최종 수정 시각';

COMMENT ON TABLE festival_highlights IS '축제 에디토리얼의 정렬된 주요 볼거리';
COMMENT ON COLUMN festival_highlights.id IS '축제 주요 볼거리 PK';
COMMENT ON COLUMN festival_highlights.festival_editorial_id IS '상위 축제 에디토리얼 id';
COMMENT ON COLUMN festival_highlights.sort_order IS '주요 볼거리 표시 순서(1부터)';
COMMENT ON COLUMN festival_highlights.title IS '주요 볼거리 타이틀';
COMMENT ON COLUMN festival_highlights.body IS '주요 볼거리 설명';
COMMENT ON COLUMN festival_highlights.created_at IS '레코드 생성 시각';
COMMENT ON COLUMN festival_highlights.updated_at IS '레코드 최종 수정 시각';
--rollback COMMENT ON TABLE festival_highlights IS NULL;
--rollback COMMENT ON TABLE festival_editorials IS NULL;
