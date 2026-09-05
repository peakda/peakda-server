--liquibase formatted sql

--changeset peakda:20260528-015-create-plant-seasons
CREATE TABLE plant_seasons (
    plant_id  BIGINT NOT NULL,
    season    TEXT   NOT NULL,
    CONSTRAINT pk_plant_seasons PRIMARY KEY (plant_id, season),
    CONSTRAINT ck_plant_seasons_season CHECK (season IN ('SPRING', 'SUMMER', 'AUTUMN_WINTER'))
);

CREATE INDEX ix_plant_seasons_season ON plant_seasons (season);
--rollback DROP TABLE plant_seasons;

--changeset peakda:20260528-015-seed-plant-seasons
INSERT INTO plant_seasons (plant_id, season)
SELECT id, 'SPRING'         FROM plants WHERE name IN ('매화','개나리','벚꽃','철쭉','진달래','유채꽃')
UNION ALL
SELECT id, 'SUMMER'         FROM plants WHERE name IN ('수국','연꽃')
UNION ALL
SELECT id, 'AUTUMN_WINTER'  FROM plants WHERE name IN ('코스모스','단풍','핑크뮬리','억새','동백꽃');
--rollback DELETE FROM plant_seasons;

--changeset peakda:20260528-015-comment-plant-seasons
COMMENT ON TABLE plant_seasons IS '식물 ↔ 주개화 계절 (N:M). 한 식물이 여러 계절 시트에 노출될 수 있도록 분리';
COMMENT ON COLUMN plant_seasons.plant_id IS 'plants.id';
COMMENT ON COLUMN plant_seasons.season IS '계절 (SPRING / SUMMER / AUTUMN_WINTER)';
--rollback COMMENT ON TABLE plant_seasons IS NULL;
