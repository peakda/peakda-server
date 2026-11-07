--liquibase formatted sql

--changeset peakda:20261107-003-allow-hidden-spot-records
ALTER TABLE spot_records DROP CONSTRAINT ck_spot_records_status;
ALTER TABLE spot_records ADD CONSTRAINT ck_spot_records_status
    CHECK (status IN ('DRAFT', 'PUBLISHED', 'HIDDEN'));
COMMENT ON TABLE spot_records IS '사용자가 스팟을 방문한 기록 (DRAFT 임시저장 / PUBLISHED 게시 / HIDDEN 숨김)';
COMMENT ON COLUMN spot_records.status IS '기록 상태 (DRAFT / PUBLISHED / HIDDEN)';
-- HIDDEN 데이터가 생긴 뒤 제약을 축소하지 않는다. 앱 rollback 시에도 확대된 CHECK를 유지한다.
--rollback empty
