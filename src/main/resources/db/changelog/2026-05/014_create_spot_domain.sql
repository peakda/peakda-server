--liquibase formatted sql

--changeset peakda:20260522-014-create-spots
CREATE TABLE spots (
    id                  BIGSERIAL        PRIMARY KEY,
    type                TEXT             NOT NULL,
    attraction_id       BIGINT,
    name                TEXT             NOT NULL,
    address             TEXT,
    latitude            DOUBLE PRECISION NOT NULL,
    longitude           DOUBLE PRECISION NOT NULL,
    kakao_place_id      TEXT,
    created_by_user_id  BIGINT,
    visible             BOOLEAN          NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ      NOT NULL,
    updated_at          TIMESTAMPTZ      NOT NULL,
    CONSTRAINT ck_spots_type CHECK (type IN ('ATTRACTION', 'LOCAL')),
    CONSTRAINT ck_spots_attraction_consistency CHECK (
        (type = 'ATTRACTION' AND attraction_id IS NOT NULL AND created_by_user_id IS NULL)
        OR (type = 'LOCAL' AND attraction_id IS NULL)
    )
);

CREATE INDEX ix_spots_attraction_id ON spots (attraction_id);
CREATE INDEX ix_spots_lat_lng ON spots (latitude, longitude);
CREATE INDEX ix_spots_kakao_place_id ON spots (kakao_place_id);

CREATE UNIQUE INDEX uk_spots_attraction_id ON spots (attraction_id) WHERE type = 'ATTRACTION';
CREATE UNIQUE INDEX uk_spots_kakao_place_id ON spots (kakao_place_id) WHERE type = 'LOCAL' AND kakao_place_id IS NOT NULL;
--rollback DROP TABLE spots;

--changeset peakda:20260522-014-create-spot-records
CREATE TABLE spot_records (
    id            BIGSERIAL   PRIMARY KEY,
    spot_id       BIGINT      NOT NULL,
    user_id       BIGINT      NOT NULL,
    visited_date  DATE,
    bloom_stage   TEXT,
    memo          TEXT,
    status        TEXT        NOT NULL,
    published_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_spot_records_status CHECK (status IN ('DRAFT', 'PUBLISHED')),
    CONSTRAINT ck_spot_records_bloom_stage CHECK (
        bloom_stage IS NULL OR bloom_stage IN ('EARLY', 'STARTING', 'PEAK', 'LATE')
    )
);

CREATE INDEX ix_spot_records_spot_id ON spot_records (spot_id);
CREATE INDEX ix_spot_records_user_id_status ON spot_records (user_id, status);
CREATE INDEX ix_spot_records_visited_date ON spot_records (visited_date);

CREATE UNIQUE INDEX uk_spot_records_user_draft ON spot_records (user_id) WHERE status = 'DRAFT';
--rollback DROP TABLE spot_records;

--changeset peakda:20260522-014-create-spot-record-photos
CREATE TABLE spot_record_photos (
    id              BIGSERIAL   PRIMARY KEY,
    spot_record_id  BIGINT      NOT NULL,
    object_key      TEXT        NOT NULL,
    sort_order      INTEGER     NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_spot_record_photos_record_sort UNIQUE (spot_record_id, sort_order),
    CONSTRAINT ck_spot_record_photos_sort_order CHECK (sort_order BETWEEN 1 AND 5)
);

CREATE INDEX ix_spot_record_photos_spot_record_id ON spot_record_photos (spot_record_id);
--rollback DROP TABLE spot_record_photos;

--changeset peakda:20260522-014-create-plants
CREATE TABLE plants (
    id                     BIGSERIAL   PRIMARY KEY,
    name                   TEXT        NOT NULL,
    sort_order             INTEGER     NOT NULL DEFAULT 0,
    status                 TEXT        NOT NULL,
    suggested_by_user_id   BIGINT,
    approved_at            TIMESTAMPTZ,
    created_at             TIMESTAMPTZ NOT NULL,
    updated_at             TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_plants_name UNIQUE (name),
    CONSTRAINT ck_plants_status CHECK (status IN ('ACTIVE', 'PENDING', 'REJECTED'))
);

CREATE INDEX ix_plants_status_sort ON plants (status, sort_order);
--rollback DROP TABLE plants;

--changeset peakda:20260522-014-create-spot-record-plants
CREATE TABLE spot_record_plants (
    spot_record_id BIGINT NOT NULL,
    plant_id       BIGINT NOT NULL,
    CONSTRAINT pk_spot_record_plants PRIMARY KEY (spot_record_id, plant_id)
);

CREATE INDEX ix_spot_record_plants_plant_id ON spot_record_plants (plant_id);
--rollback DROP TABLE spot_record_plants;

--changeset peakda:20260522-014-seed-plants
INSERT INTO plants (name, sort_order, status, approved_at, created_at, updated_at) VALUES
    ('동백꽃',   1,  'ACTIVE', now(), now(), now()),
    ('매화',     2,  'ACTIVE', now(), now(), now()),
    ('개나리',   3,  'ACTIVE', now(), now(), now()),
    ('벚꽃',     4,  'ACTIVE', now(), now(), now()),
    ('철쭉',     5,  'ACTIVE', now(), now(), now()),
    ('진달래',   6,  'ACTIVE', now(), now(), now()),
    ('유채꽃',   7,  'ACTIVE', now(), now(), now()),
    ('수국',     8,  'ACTIVE', now(), now(), now()),
    ('연꽃',     9,  'ACTIVE', now(), now(), now()),
    ('코스모스', 10, 'ACTIVE', now(), now(), now()),
    ('단풍',     11, 'ACTIVE', now(), now(), now()),
    ('핑크뮬리', 12, 'ACTIVE', now(), now(), now()),
    ('억새',     13, 'ACTIVE', now(), now(), now());
--rollback DELETE FROM plants WHERE name IN ('동백꽃','매화','개나리','벚꽃','철쭉','진달래','유채꽃','수국','연꽃','코스모스','단풍','핑크뮬리','억새');

--changeset peakda:20260522-014-comment-spot-domain
COMMENT ON TABLE spots IS '지도/검색 단위 스팟 (유명 명소 ATTRACTION 또는 사용자 발견 동네 LOCAL)';
COMMENT ON COLUMN spots.id IS '스팟 PK';
COMMENT ON COLUMN spots.type IS '스팟 분류 (ATTRACTION 유명 명소 / LOCAL 동네 스팟)';
COMMENT ON COLUMN spots.attraction_id IS 'ATTRACTION 일 때 attractions.id 참조 (LOCAL 은 NULL)';
COMMENT ON COLUMN spots.name IS '표시명 (ATTRACTION 도 복제 저장하여 단일 테이블 검색 지원)';
COMMENT ON COLUMN spots.address IS '도로명/지번 주소';
COMMENT ON COLUMN spots.latitude IS '위도';
COMMENT ON COLUMN spots.longitude IS '경도';
COMMENT ON COLUMN spots.kakao_place_id IS '카카오 장소 식별자 (있을 때만, LOCAL 중복 탐지에 사용)';
COMMENT ON COLUMN spots.created_by_user_id IS 'LOCAL 스팟 생성자 user id (ATTRACTION 은 NULL)';
COMMENT ON COLUMN spots.visible IS '서비스 노출 여부';
COMMENT ON COLUMN spots.created_at IS '레코드 생성 시각';
COMMENT ON COLUMN spots.updated_at IS '레코드 최종 수정 시각';

COMMENT ON TABLE spot_records IS '사용자가 스팟을 방문한 기록 (DRAFT 임시저장 또는 PUBLISHED 게시)';
COMMENT ON COLUMN spot_records.id IS '기록 PK';
COMMENT ON COLUMN spot_records.spot_id IS 'spots.id';
COMMENT ON COLUMN spot_records.user_id IS '기록을 작성한 사용자 id';
COMMENT ON COLUMN spot_records.visited_date IS '방문 일자 (PUBLISHED 시 not null)';
COMMENT ON COLUMN spot_records.bloom_stage IS '개화/단풍 상태 (EARLY/STARTING/PEAK/LATE, PUBLISHED 시 not null)';
COMMENT ON COLUMN spot_records.memo IS '메모 (최대 1000자, 애플리케이션 단 검증)';
COMMENT ON COLUMN spot_records.status IS '기록 상태 (DRAFT / PUBLISHED)';
COMMENT ON COLUMN spot_records.published_at IS 'PUBLISHED 전환 시각';
COMMENT ON COLUMN spot_records.created_at IS '레코드 생성 시각';
COMMENT ON COLUMN spot_records.updated_at IS '레코드 최종 수정 시각';

COMMENT ON TABLE spot_record_photos IS '스팟 기록에 첨부된 사진 (사용자당 1~5장)';
COMMENT ON COLUMN spot_record_photos.id IS '사진 PK';
COMMENT ON COLUMN spot_record_photos.spot_record_id IS 'spot_records.id';
COMMENT ON COLUMN spot_record_photos.object_key IS 'ObjectStorage 저장 key (응답 시점에 presigned URL 변환)';
COMMENT ON COLUMN spot_record_photos.sort_order IS '표시 순서 (1~5, 1이 대표)';
COMMENT ON COLUMN spot_record_photos.created_at IS '레코드 생성 시각';
COMMENT ON COLUMN spot_record_photos.updated_at IS '레코드 최종 수정 시각';

COMMENT ON TABLE plants IS '식물 마스터 (ACTIVE) 및 사용자 제안 (PENDING)';
COMMENT ON COLUMN plants.id IS '식물 PK';
COMMENT ON COLUMN plants.name IS '식물 이름 (전역 유일)';
COMMENT ON COLUMN plants.sort_order IS '마스터 칩 표시 순서';
COMMENT ON COLUMN plants.status IS '식물 상태 (ACTIVE 노출 / PENDING 제안 / REJECTED 거절)';
COMMENT ON COLUMN plants.suggested_by_user_id IS '사용자 제안일 때 제안자 user id';
COMMENT ON COLUMN plants.approved_at IS 'ACTIVE 전환 시각';
COMMENT ON COLUMN plants.created_at IS '레코드 생성 시각';
COMMENT ON COLUMN plants.updated_at IS '레코드 최종 수정 시각';

COMMENT ON TABLE spot_record_plants IS '스팟 기록 ↔ 식물 다대다 조인';
COMMENT ON COLUMN spot_record_plants.spot_record_id IS 'spot_records.id';
COMMENT ON COLUMN spot_record_plants.plant_id IS 'plants.id';
--rollback COMMENT ON TABLE spots IS NULL;
