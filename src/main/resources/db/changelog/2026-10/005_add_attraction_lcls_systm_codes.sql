--liquibase formatted sql

--changeset peakda:20261001-005-add-attraction-lcls-systm-codes
ALTER TABLE attractions ADD COLUMN lcls_systm_major TEXT;
ALTER TABLE attractions ADD COLUMN lcls_systm_medium TEXT;
ALTER TABLE attractions ADD COLUMN lcls_systm_minor TEXT;

COMMENT ON COLUMN attractions.lcls_systm_major IS 'TourAPI 신 분류체계 대분류 (lclsSystm1). 기존 cat1~3 은 최근 데이터에서 비어 있다';
COMMENT ON COLUMN attractions.lcls_systm_medium IS 'TourAPI 신 분류체계 중분류 (lclsSystm2)';
COMMENT ON COLUMN attractions.lcls_systm_minor IS 'TourAPI 신 분류체계 소분류 (lclsSystm3)';
--rollback ALTER TABLE attractions DROP COLUMN lcls_systm_minor; ALTER TABLE attractions DROP COLUMN lcls_systm_medium; ALTER TABLE attractions DROP COLUMN lcls_systm_major;
