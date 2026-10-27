--liquibase formatted sql

--changeset peakda:20261027-015-add-curations-pinned-at
ALTER TABLE curations
    ADD COLUMN pinned_at TIMESTAMPTZ;

COMMENT ON COLUMN curations.pinned_at IS '큐레이션 탐색 상단 고정 시각. NULL이면 고정 해제';
--rollback ALTER TABLE curations DROP COLUMN pinned_at;
