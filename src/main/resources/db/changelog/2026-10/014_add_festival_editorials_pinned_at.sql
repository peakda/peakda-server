--liquibase formatted sql

--changeset peakda:20261027-014-add-festival-editorials-pinned-at
ALTER TABLE festival_editorials
    ADD COLUMN pinned_at TIMESTAMPTZ;

COMMENT ON COLUMN festival_editorials.pinned_at IS '축제 에디토리얼 탐색 상단 고정 시각. NULL이면 고정 해제';
--rollback ALTER TABLE festival_editorials DROP COLUMN pinned_at;
