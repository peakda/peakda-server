--liquibase formatted sql

--changeset peakda:20260514-011-add-scheduler-job-run-skip
ALTER TABLE scheduler_job_runs
    ADD COLUMN skip_reason TEXT;

ALTER TABLE scheduler_job_runs
    DROP CONSTRAINT ck_scheduler_job_runs_status;

ALTER TABLE scheduler_job_runs
    ADD CONSTRAINT ck_scheduler_job_runs_status
        CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED', 'SKIPPED'));

CREATE INDEX idx_scheduler_job_runs_skipped ON scheduler_job_runs (job_name, started_at DESC) WHERE status = 'SKIPPED';

--rollback DROP INDEX IF EXISTS idx_scheduler_job_runs_skipped;
--rollback ALTER TABLE scheduler_job_runs DROP CONSTRAINT ck_scheduler_job_runs_status;
--rollback ALTER TABLE scheduler_job_runs ADD CONSTRAINT ck_scheduler_job_runs_status CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED'));
--rollback ALTER TABLE scheduler_job_runs DROP COLUMN skip_reason;
