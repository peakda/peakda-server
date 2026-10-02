--liquibase formatted sql

--changeset peakda:20261002-007-allow-gallery-tag-source
ALTER TABLE attraction_blooms DROP CONSTRAINT ck_attraction_blooms_source;
ALTER TABLE attraction_blooms ADD CONSTRAINT ck_attraction_blooms_source CHECK (
    source IN ('KEYWORD', 'FESTIVAL', 'CATEGORY', 'OBSERVATION', 'GALLERY', 'MANUAL', 'EXIF_BOOST')
);
COMMENT ON COLUMN attraction_blooms.source IS '태깅 출처 (KEYWORD/FESTIVAL/CATEGORY/OBSERVATION/GALLERY/MANUAL/EXIF_BOOST)';
--rollback DELETE FROM attraction_blooms WHERE source = 'GALLERY';
--rollback ALTER TABLE attraction_blooms DROP CONSTRAINT ck_attraction_blooms_source;
--rollback ALTER TABLE attraction_blooms ADD CONSTRAINT ck_attraction_blooms_source CHECK (source IN ('KEYWORD', 'FESTIVAL', 'CATEGORY', 'OBSERVATION', 'MANUAL', 'EXIF_BOOST'));
--rollback COMMENT ON COLUMN attraction_blooms.source IS '태깅 출처 (KEYWORD/FESTIVAL/CATEGORY/OBSERVATION/MANUAL/EXIF_BOOST)';
