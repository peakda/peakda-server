--liquibase formatted sql

--changeset peakda:20260920-002-allow-before-season-bloom-status
ALTER TABLE seasonal_bloom_estimates DROP CONSTRAINT ck_seasonal_bloom_estimates_status;
ALTER TABLE seasonal_bloom_estimates ADD CONSTRAINT ck_seasonal_bloom_estimates_status CHECK (
    status IN ('BEFORE_SEASON', 'PREPARING', 'STARTED', 'PEAK', 'ENDED')
);
--rollback ALTER TABLE seasonal_bloom_estimates DROP CONSTRAINT ck_seasonal_bloom_estimates_status;
--rollback UPDATE seasonal_bloom_estimates SET status = 'PREPARING' WHERE status = 'BEFORE_SEASON';
--rollback ALTER TABLE seasonal_bloom_estimates ADD CONSTRAINT ck_seasonal_bloom_estimates_status CHECK (status IN ('PREPARING', 'STARTED', 'PEAK', 'ENDED'));
