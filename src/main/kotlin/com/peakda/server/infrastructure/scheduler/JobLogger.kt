package com.peakda.server.infrastructure.scheduler

import com.peakda.server.infrastructure.scheduler.history.SchedulerJobRunRecorder
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class JobLogger(
    private val recorder: SchedulerJobRunRecorder,
    private val meterRegistry: MeterRegistry,
) {
    /** [enabled] 가 false 면 아무 일도 하지 않고 종료한다. 모든 SyncJob 의 표준 진입점. */
    fun runIfEnabled(jobName: String, enabled: Boolean, block: () -> Map<String, Any?>) {
        if (!enabled) return
        run(jobName, block)
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
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - started
            sample.stop(meterRegistry.timer("scheduler.job.duration", "job", jobName))
            recorder.fail(runId, e)
            meterRegistry.counter("scheduler.job.failure_total", "job", jobName).increment()
            log.error("[scheduler] job={} status=FAILED ms={} error={}", jobName, elapsed, e.message, e)
        }
    }

    companion object {
        const val KEY_PROCESSED = "processedCount"
        const val KEY_TOTAL = "totalCount"
        private val log = LoggerFactory.getLogger(JobLogger::class.java)
    }
}
