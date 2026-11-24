--liquibase formatted sql

--changeset peakda:20261125-005-allow-sunrise-festival-kind
ALTER TABLE festivals DROP CONSTRAINT ck_festivals_kind;
ALTER TABLE festivals ADD CONSTRAINT ck_festivals_kind CHECK (kind IN ('FESTIVAL', 'TREE', 'SUNRISE'));
COMMENT ON COLUMN festivals.kind IS '축제 종류. FESTIVAL(축제·행사)·TREE(트리 명소)·SUNRISE(일출 명소)';

--rollback ALTER TABLE festivals DROP CONSTRAINT ck_festivals_kind;
--rollback ALTER TABLE festivals ADD CONSTRAINT ck_festivals_kind CHECK (kind IN ('FESTIVAL', 'TREE'));
