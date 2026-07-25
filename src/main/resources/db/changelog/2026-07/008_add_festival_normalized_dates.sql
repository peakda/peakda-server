--liquibase formatted sql

--changeset peakda:20260725-008-add-festival-normalized-dates
ALTER TABLE festivals
    ADD COLUMN starts_on DATE,
    ADD COLUMN ends_on   DATE;

-- 원천 TEXT 는 `20260501`·`2026-05-01` 등 구분자가 섞여 오므로 숫자만 남겨 환산한다.
-- 월·일 범위까지 정규식으로 걸러야 한다: to_date 는 `20260001`(월 00) 에서 에러를 던져 마이그레이션을
-- 중단시키고, `20261301` 은 조용히 다음 해로 굴려 앱의 strict 파서(NULL)와 결과가 갈린다.
-- 범위를 벗어난 값은 NULL 로 남겨 "파싱 불가 시 NULL" 규약을 SQL·앱 양쪽에서 동일하게 유지한다.
UPDATE festivals
SET starts_on = to_date(regexp_replace(start_date, '\D', '', 'g'), 'YYYYMMDD')
WHERE regexp_replace(start_date, '\D', '', 'g') ~ '^\d{4}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])$';

UPDATE festivals
SET ends_on = to_date(regexp_replace(end_date, '\D', '', 'g'), 'YYYYMMDD')
WHERE end_date IS NOT NULL
  AND regexp_replace(end_date, '\D', '', 'g') ~ '^\d{4}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])$';

CREATE INDEX ix_festivals_starts_on ON festivals (starts_on);
--rollback DROP INDEX ix_festivals_starts_on;
--rollback ALTER TABLE festivals DROP COLUMN ends_on;
--rollback ALTER TABLE festivals DROP COLUMN starts_on;

--changeset peakda:20260725-008-comment-festival-normalized-dates
COMMENT ON COLUMN festivals.starts_on IS '축제 시작일 (원천 start_date TEXT 를 DATE 로 정규화, 파싱 불가 시 NULL)';
COMMENT ON COLUMN festivals.ends_on IS '축제 종료일 (원천 end_date TEXT 를 DATE 로 정규화, 파싱 불가 시 NULL)';
--rollback COMMENT ON COLUMN festivals.starts_on IS NULL;
--rollback COMMENT ON COLUMN festivals.ends_on IS NULL;
