--liquibase formatted sql

--changeset peakda:20261001-002-add-attraction-legal-dong-codes
ALTER TABLE attractions ADD COLUMN legal_dong_area_code TEXT;
ALTER TABLE attractions ADD COLUMN legal_dong_sigungu_code TEXT;
CREATE INDEX idx_attractions_legal_dong_sigungu_code ON attractions (legal_dong_sigungu_code);

COMMENT ON COLUMN attractions.legal_dong_area_code IS '법정동 시도 코드 2자리 (TourAPI lDongRegnCd). 행정구역 개편 후 코드';
COMMENT ON COLUMN attractions.legal_dong_sigungu_code IS '법정동 시군구 코드 5자리 (TourAPI lDongRegnCd + lDongSignguCd). 관광지 집중률 연결 키';
--rollback DROP INDEX idx_attractions_legal_dong_sigungu_code; ALTER TABLE attractions DROP COLUMN legal_dong_sigungu_code; ALTER TABLE attractions DROP COLUMN legal_dong_area_code;
