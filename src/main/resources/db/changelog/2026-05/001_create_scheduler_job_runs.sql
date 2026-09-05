--liquibase formatted sql

--changeset peakda:20260513-001-create-scheduler-job-runs
CREATE TABLE scheduler_job_runs (
    id              BIGSERIAL   PRIMARY KEY,
    job_name        TEXT        NOT NULL,
    started_at      TIMESTAMPTZ NOT NULL,
    finished_at     TIMESTAMPTZ,
    status          TEXT        NOT NULL,
    processed_count INTEGER,
    total_count     INTEGER,
    error_message   TEXT,
    error_stack     TEXT,
    CONSTRAINT ck_scheduler_job_runs_status CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_scheduler_job_runs_job_started_at ON scheduler_job_runs (job_name, started_at DESC);
CREATE INDEX idx_scheduler_job_runs_failed ON scheduler_job_runs (job_name, started_at DESC) WHERE status = 'FAILED';
--rollback DROP TABLE scheduler_job_runs;
