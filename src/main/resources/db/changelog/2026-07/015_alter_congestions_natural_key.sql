--liquibase formatted sql

--changeset peakda:20260730-015-alter-congestions-natural-key
-- 집중률 API(tatsCnctrRatedList) 응답에는 관광지 코드가 없고 관광지명만 온다.
-- tourist_attraction_code 는 채워질 수 없는 컬럼이므로 제거하고,
-- 자연키를 (기준일자, 지역, 시군구, 관광지명) 으로 옮긴다.
DELETE FROM congestions
WHERE area_code IS NULL
   OR sigungu_code IS NULL
   OR tourist_attraction_name IS NULL;

ALTER TABLE congestions
    DROP CONSTRAINT uk_congestions_date_attraction,
    DROP COLUMN tourist_attraction_code,
    ALTER COLUMN area_code SET NOT NULL,
    ALTER COLUMN sigungu_code SET NOT NULL,
    ALTER COLUMN tourist_attraction_name SET NOT NULL,
    ADD CONSTRAINT uk_congestions_date_region_attraction
        UNIQUE (base_date, area_code, sigungu_code, tourist_attraction_name);

COMMENT ON COLUMN congestions.area_code IS '지역 코드 (집중률 API areaCd)';
COMMENT ON COLUMN congestions.sigungu_code IS '시군구 코드 (집중률 API signguCd)';
COMMENT ON COLUMN congestions.tourist_attraction_name IS '관광지 명칭 (집중률 API 가 제공하는 유일한 관광지 식별자)';

--rollback ALTER TABLE congestions DROP CONSTRAINT uk_congestions_date_region_attraction;
--rollback ALTER TABLE congestions ALTER COLUMN tourist_attraction_name DROP NOT NULL;
--rollback ALTER TABLE congestions ALTER COLUMN sigungu_code DROP NOT NULL;
--rollback ALTER TABLE congestions ALTER COLUMN area_code DROP NOT NULL;
--rollback ALTER TABLE congestions ADD COLUMN tourist_attraction_code TEXT NOT NULL DEFAULT '';
--rollback ALTER TABLE congestions ADD CONSTRAINT uk_congestions_date_attraction UNIQUE (base_date, tourist_attraction_code);
