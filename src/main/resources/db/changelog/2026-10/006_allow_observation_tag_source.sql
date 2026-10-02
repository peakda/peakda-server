--liquibase formatted sql

--changeset peakda:20261002-006-allow-observation-tag-source
ALTER TABLE attraction_blooms DROP CONSTRAINT ck_attraction_blooms_source;
ALTER TABLE attraction_blooms ADD CONSTRAINT ck_attraction_blooms_source CHECK (
    source IN ('KEYWORD', 'FESTIVAL', 'CATEGORY', 'OBSERVATION', 'MANUAL', 'EXIF_BOOST')
);
COMMENT ON COLUMN attraction_blooms.source IS '태깅 출처 (KEYWORD/FESTIVAL/CATEGORY/OBSERVATION/MANUAL/EXIF_BOOST)';
--rollback DELETE FROM attraction_blooms WHERE source = 'OBSERVATION';
--rollback ALTER TABLE attraction_blooms DROP CONSTRAINT ck_attraction_blooms_source;
--rollback ALTER TABLE attraction_blooms ADD CONSTRAINT ck_attraction_blooms_source CHECK (source IN ('KEYWORD', 'FESTIVAL', 'CATEGORY', 'MANUAL', 'EXIF_BOOST'));
--rollback COMMENT ON COLUMN attraction_blooms.source IS '태깅 출처 (KEYWORD/FESTIVAL/CATEGORY/MANUAL/EXIF_BOOST)';
