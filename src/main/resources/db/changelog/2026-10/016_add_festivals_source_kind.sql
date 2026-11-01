--liquibase formatted sql

--changeset peakda:20261031-016-add-festivals-source-kind
ALTER TABLE festivals
    ADD COLUMN source TEXT NOT NULL DEFAULT 'PUBLIC_DATA',
    ADD COLUMN kind   TEXT NOT NULL DEFAULT 'FESTIVAL',
    ADD CONSTRAINT ck_festivals_source CHECK (source IN ('PUBLIC_DATA', 'MANUAL')),
    ADD CONSTRAINT ck_festivals_kind CHECK (kind IN ('FESTIVAL', 'TREE'));

CREATE INDEX ix_festivals_kind_starts_on ON festivals (kind, starts_on);

COMMENT ON COLUMN festivals.source IS '축제 데이터 출처. PUBLIC_DATA(공공데이터 동기화)·MANUAL(관리자 직접 등록). MANUAL 행은 동기화가 덮어쓰지 않는다';
COMMENT ON COLUMN festivals.kind IS '축제 종류. FESTIVAL(축제·행사)·TREE(트리 명소)';
--rollback DROP INDEX ix_festivals_kind_starts_on;
--rollback ALTER TABLE festivals DROP COLUMN kind;
--rollback ALTER TABLE festivals DROP COLUMN source;
