package com.peakda.server.infrastructure.scheduler.history

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(
    name = "scheduler_job_runs",
    indexes = [
        Index(name = "idx_scheduler_job_runs_job_started_at", columnList = "job_name, started_at"),
    ],
)
class SchedulerJobRun(
    @Column(name = "job_name", nullable = false, columnDefinition = "TEXT")
    val jobName: String,

    @Column(name = "started_at", nullable = false)
    val startedAt: Instant,

    @Column(name = "finished_at")
    var finishedAt: Instant? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "TEXT")
    var status: SchedulerJobStatus = SchedulerJobStatus.RUNNING,

    @Column(name = "processed_count")
    var processedCount: Int? = null,

    @Column(name = "total_count")
    var totalCount: Int? = null,

    @Column(name = "error_message", columnDefinition = "TEXT")
    var errorMessage: String? = null,

    @Column(name = "error_stack", columnDefinition = "TEXT")
    var errorStack: String? = null,

    @Column(name = "skip_reason", columnDefinition = "TEXT")
    var skipReason: String? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set

    fun complete(finishedAt: Instant, processedCount: Int?, totalCount: Int?) {
        this.status = SchedulerJobStatus.COMPLETED
        this.finishedAt = finishedAt
        this.processedCount = processedCount
        this.totalCount = totalCount
    }

    fun fail(finishedAt: Instant, errorMessage: String?, errorStack: String?) {
        this.status = SchedulerJobStatus.FAILED
        this.finishedAt = finishedAt
        this.errorMessage = errorMessage
        this.errorStack = errorStack
    }

    fun skip(finishedAt: Instant, reason: String) {
        this.status = SchedulerJobStatus.SKIPPED
        this.finishedAt = finishedAt
        this.skipReason = reason
    }
}

enum class SchedulerJobStatus {
    RUNNING,
    COMPLETED,
    FAILED,
    SKIPPED,
}
