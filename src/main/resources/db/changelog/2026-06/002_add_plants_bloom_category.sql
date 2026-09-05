--liquibase formatted sql

--changeset peakda:20260606-002-add-plants-bloom-category
ALTER TABLE plants ADD COLUMN bloom_category TEXT;
ALTER TABLE plants ADD CONSTRAINT ck_plants_bloom_category CHECK (
    bloom_category IS NULL OR bloom_category IN (
        'PLUM', 'FORSYTHIA', 'AZALEA_KR', 'CHERRY', 'CANOLA', 'AZALEA',
        'HYDRANGEA', 'LOTUS', 'COSMOS', 'PINK_MUHLY', 'SILVERGRASS', 'MAPLE', 'CAMELLIA'
    )
);
--rollback ALTER TABLE plants DROP COLUMN bloom_category;

--changeset peakda:20260606-002-seed-plants-bloom-category
UPDATE plants SET bloom_category = 'CAMELLIA'    WHERE name = '동백꽃';
UPDATE plants SET bloom_category = 'PLUM'        WHERE name = '매화';
UPDATE plants SET bloom_category = 'FORSYTHIA'   WHERE name = '개나리';
UPDATE plants SET bloom_category = 'CHERRY'      WHERE name = '벚꽃';
UPDATE plants SET bloom_category = 'AZALEA'      WHERE name = '철쭉';
UPDATE plants SET bloom_category = 'AZALEA_KR'   WHERE name = '진달래';
UPDATE plants SET bloom_category = 'CANOLA'      WHERE name = '유채꽃';
UPDATE plants SET bloom_category = 'HYDRANGEA'   WHERE name = '수국';
UPDATE plants SET bloom_category = 'LOTUS'       WHERE name = '연꽃';
UPDATE plants SET bloom_category = 'COSMOS'      WHERE name = '코스모스';
UPDATE plants SET bloom_category = 'MAPLE'       WHERE name = '단풍';
UPDATE plants SET bloom_category = 'PINK_MUHLY'  WHERE name = '핑크뮬리';
UPDATE plants SET bloom_category = 'SILVERGRASS' WHERE name = '억새';
--rollback UPDATE plants SET bloom_category = NULL;

--changeset peakda:20260606-002-comment-plants-bloom-category
COMMENT ON COLUMN plants.bloom_category IS '표준 꽃·계절 카테고리(BloomCategory) 매핑 — 타이밍 도메인 브릿지, 미매핑 시 NULL';
--rollback COMMENT ON COLUMN plants.bloom_category IS NULL;
