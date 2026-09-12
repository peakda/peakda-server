--liquibase formatted sql

--changeset peakda:20260912-001-allow-observation-bloom-estimator
ALTER TABLE seasonal_bloom_estimates DROP CONSTRAINT ck_seasonal_bloom_estimates_estimator;
ALTER TABLE seasonal_bloom_estimates ADD CONSTRAINT ck_seasonal_bloom_estimates_estimator CHECK (
    chosen_estimator IN ('GDD', 'FESTIVAL', 'CALENDAR', 'USER_RECORD', 'OBSERVATION')
);
--rollback ALTER TABLE seasonal_bloom_estimates DROP CONSTRAINT ck_seasonal_bloom_estimates_estimator;
--rollback ALTER TABLE seasonal_bloom_estimates ADD CONSTRAINT ck_seasonal_bloom_estimates_estimator CHECK (chosen_estimator IN ('GDD', 'FESTIVAL', 'CALENDAR', 'USER_RECORD'));
