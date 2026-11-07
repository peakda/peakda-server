--liquibase formatted sql

--changeset peakda:20261107-002-register-spot-record-photo-uploads
CREATE TABLE spot_record_photo_uploads (
    object_key TEXT PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    variant_names TEXT NOT NULL,
    evidence VARCHAR(64) NOT NULL,
    available BOOLEAN NOT NULL DEFAULT true,
    CONSTRAINT ck_spot_record_photo_upload_evidence
        CHECK (evidence IN ('SERVER_UPLOAD', 'LEGACY_SERVER_KEY_AND_STORAGE'))
);
CREATE INDEX idx_spot_record_photos_object_key ON spot_record_photos (object_key);
COMMENT ON TABLE spot_record_photo_uploads IS '서버 업로드 또는 서버 key 정책과 스토리지 실재 확인으로 등록한 사진 소유 증거. 기존 association은 자동 backfill하지 않음';
--rollback DROP INDEX idx_spot_record_photos_object_key;
--rollback DROP TABLE spot_record_photo_uploads;
