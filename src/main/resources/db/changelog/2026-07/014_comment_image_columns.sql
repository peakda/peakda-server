--liquibase formatted sql

--changeset peakda:20260728-014-comment-image-columns
COMMENT ON COLUMN curations.hero_image_url IS '큐레이션 히어로 이미지 object key 또는 외부 URL';
COMMENT ON COLUMN curation_chapters.photo_url IS '큐레이션 챕터 사진 object key 또는 외부 URL';
COMMENT ON COLUMN curation_recommendations.photo_url IS '큐레이션 추천 카드 사진 object key 또는 외부 URL';
COMMENT ON COLUMN festival_editorials.hero_image_url IS '축제 상세 히어로 이미지 object key 또는 외부 URL';

--rollback COMMENT ON COLUMN festival_editorials.hero_image_url IS '축제 상세 히어로 이미지 URL';
--rollback COMMENT ON COLUMN curation_recommendations.photo_url IS '추천 카드 사진 URL';
--rollback COMMENT ON COLUMN curation_chapters.photo_url IS '챕터 사진 URL';
--rollback COMMENT ON COLUMN curations.hero_image_url IS '상세 히어로 이미지 URL';
