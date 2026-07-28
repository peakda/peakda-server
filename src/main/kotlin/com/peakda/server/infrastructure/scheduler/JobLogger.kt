package com.peakda.server.infrastructure.scheduler

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.infrastructure.external.common.ExternalApiException
import com.peakda.server.infrastructure.scheduler.history.SchedulerJobRunRecorder
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class JobLogger(
    private val recorder: SchedulerJobRunRecorder,
    private val meterRegistry: MeterRegistry,
    private val lock: SchedulerJobLock,
) {
    /** [enabled] 가 false 면 아무 일도 하지 않고 종료한다. 모든 SyncJob 의 크론 진입점. */
    fun runIfEnabled(jobName: String, enabled: Boolean, block: () -> Map<String, Any?>) {
        if (!enabled) {
            log.debug("[scheduler] job={} status=DISABLED", jobName)
            return
        }
        runWithLock(jobName, block)
    }

    /**
     * 관리자 수동 실행 진입점. [runIfEnabled] 와 달리 enabled 플래그를 검사하지 않는다.
     *
     * 플래그의 의미는 "크론이 자동으로 돌지 말라"이지 "운영자가 수동으로도 못 돌린다"가 아니다.
     * 락 획득과 실행 이력 적재는 크론 경로와 동일하게 동작하므로, 꺼진 잡을 수동 실행해도
     * `scheduler_job_runs` 에 결과가 남는다.
     */
    fun runManually(jobName: String, block: () -> Map<String, Any?>) {
        log.info("[scheduler] job={} trigger=MANUAL", jobName)
        runWithLock(jobName, block)
    }

    private fun runWithLock(jobName: String, block: () -> Map<String, Any?>) {
        when (lock.withLock(jobName) { run(jobName, block) }) {
            is SchedulerJobLockResult.Acquired -> Unit
            SchedulerJobLockResult.Locked -> skip(jobName, SKIP_LOCKED)
        }
    }

    fun run(jobName: String, block: () -> Map<String, Any?>) {
        val runId = recorder.start(jobName)
        log.info("[scheduler] job={} status=STARTED", jobName)
        val sample = Timer.start(meterRegistry)
        val started = System.currentTimeMillis()
        try {
            val result = block()
            val elapsed = System.currentTimeMillis() - started
            sample.stop(meterRegistry.timer("scheduler.job.duration", "job", jobName))
            val processed = (result[KEY_PROCESSED] as? Number)?.toInt()
            val total = (result[KEY_TOTAL] as? Number)?.toInt()
            recorder.complete(runId, processed, total)
            meterRegistry.counter("scheduler.job.success_total", "job", jobName).increment()
            val extras = result.entries.joinToString(" ") { "${it.key}=${it.value}" }
            if (extras.isNotEmpty()) {
                log.info("[scheduler] job={} status=COMPLETED ms={} {}", jobName, elapsed, extras)
            } else {
                log.info("[scheduler] job={} status=COMPLETED ms={}", jobName, elapsed)
            }
        } catch (e: ExternalApiException) {
            val elapsed = System.currentTimeMillis() - started
            sample.stop(meterRegistry.timer("scheduler.job.duration", "job", jobName))
            if (e.errorCode == ErrorCode.EXTERNAL_API_QUOTA_EXCEEDED) {
                recorder.skipExisting(runId, SKIP_QUOTA_EXHAUSTED)
                meterRegistry.counter(
                    "scheduler.job.skip_total",
                    "job", jobName,
                    "reason", SKIP_QUOTA_EXHAUSTED,
                ).increment()
                log.warn(
                    "[scheduler] job={} status=SKIPPED reason={} ms={} message={}",
                    jobName, SKIP_QUOTA_EXHAUSTED, elapsed, e.message,
                )
            } else {
                recorder.fail(runId, e)
                meterRegistry.counter("scheduler.job.failure_total", "job", jobName).increment()
                log.error("[scheduler] job={} status=FAILED ms={} error={}", jobName, elapsed, e.message, e)
            }
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - started
            sample.stop(meterRegistry.timer("scheduler.job.duration", "job", jobName))
            recorder.fail(runId, e)
            meterRegistry.counter("scheduler.job.failure_total", "job", jobName).increment()
            log.error("[scheduler] job={} status=FAILED ms={} error={}", jobName, elapsed, e.message, e)
        }
    }

    private fun skip(jobName: String, reason: String) {
        recorder.skip(jobName, reason)
        meterRegistry.counter("scheduler.job.skip_total", "job", jobName, "reason", reason).increment()
        log.info("[scheduler] job={} status=SKIPPED reason={}", jobName, reason)
    }

    companion object {
        const val KEY_PROCESSED = "processedCount"
        const val KEY_TOTAL = "totalCount"
        const val SKIP_LOCKED = "locked"
        const val SKIP_QUOTA_EXHAUSTED = "quota_exhausted"
        private val log = LoggerFactory.getLogger(JobLogger::class.java)
    }
}
