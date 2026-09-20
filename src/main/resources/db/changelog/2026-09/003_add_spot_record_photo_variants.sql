--liquibase formatted sql

--changeset peakda:20260920-003-add-spot-record-photo-variants
ALTER TABLE spot_record_photos ADD COLUMN variant_names TEXT;

COMMENT ON COLUMN spot_record_photos.variant_names IS '보유한 이미지 variant 이름 목록(콤마 구분). NULL 은 thumbnail·main 두 벌만 있던 과거 업로드';
--rollback ALTER TABLE spot_record_photos DROP COLUMN variant_names;
