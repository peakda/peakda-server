package com.peakda.server.infrastructure.scheduler.spot

import com.peakda.server.domain.spot.application.SpotRecordPhotoDeletionProperties
import com.peakda.server.domain.spot.application.SpotRecordPhotoDeletionService
import com.peakda.server.infrastructure.scheduler.JobLogger
import com.peakda.server.infrastructure.scheduler.ManualTriggerableJob
import com.peakda.server.infrastructure.scheduler.SchedulerProperties
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class SpotRecordPhotoDeletionJob(
    private val service: SpotRecordPhotoDeletionService,
    private val properties: SpotRecordPhotoDeletionProperties,
    private val scheduler: SchedulerProperties,
    private val logger: JobLogger,
) : ManualTriggerableJob {
    override val jobName = "spotRecordPhotoDeletion"

    @Scheduled(fixedDelayString = "\${spot.photo-deletion.fixed-delay:30s}")
    fun run() {
        if (!scheduler.enabled || !properties.enabled) return
        // 행별 claim/lease가 중복 실행을 제어한다. 느린 외부 호출이 다른 노드의 poll을 막지 않는다.
        logger.run(jobName) { execute() }
    }

    override fun runNow() {
        logger.runManually(jobName) { execute() }
    }

    private fun execute(): Map<String, Any?> {
        val processed = service.drain()
        return mapOf(JobLogger.KEY_PROCESSED to processed, "outstanding" to service.outstandingCounts())
    }
}
