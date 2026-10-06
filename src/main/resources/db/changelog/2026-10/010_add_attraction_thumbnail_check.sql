--liquibase formatted sql

--changeset peakda:20261006-010-add-attraction-thumbnail-check
ALTER TABLE attractions
    ADD COLUMN thumbnail_checked_url TEXT,
    ADD COLUMN thumbnail_checked_at  TIMESTAMPTZ,
    ADD COLUMN thumbnail_missing     BOOLEAN NOT NULL DEFAULT false;

COMMENT ON COLUMN attractions.thumbnail_checked_url IS '마지막으로 열리는지 확인한 썸네일 URL. thumbnail_image_url 과 다르면 아직 확인하지 않은 것이다';
COMMENT ON COLUMN attractions.thumbnail_checked_at IS '썸네일 URL 을 마지막으로 확인한 시각';
COMMENT ON COLUMN attractions.thumbnail_missing IS 'thumbnail_checked_url 이 관광공사 이미지 서버에서 404/410 이었는지. true 면 카드에 원본 이미지를 쓴다';
--rollback ALTER TABLE attractions DROP COLUMN thumbnail_missing, DROP COLUMN thumbnail_checked_at, DROP COLUMN thumbnail_checked_url;
