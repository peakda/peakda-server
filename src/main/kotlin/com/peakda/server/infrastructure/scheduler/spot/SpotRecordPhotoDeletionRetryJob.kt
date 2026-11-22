package com.peakda.server.infrastructure.scheduler.spot

import com.peakda.server.domain.spot.application.SpotRecordPhotoDeletionService
import com.peakda.server.infrastructure.scheduler.JobLogger
import com.peakda.server.infrastructure.scheduler.ManualTriggerableJob
import org.springframework.stereotype.Component

/** 자동 스케줄은 없다. 기존 관리자 권한/감사·잡 이력 경로에서만 명시적으로 FAILED를 재예약한다. */
@Component
class SpotRecordPhotoDeletionRetryJob(
    private val service: SpotRecordPhotoDeletionService,
    private val logger: JobLogger,
) : ManualTriggerableJob {
    override val jobName = "spotRecordPhotoDeletionRetryFailed"

    override fun runNow() {
        logger.runManually(jobName) {
            mapOf("requeued" to service.retryFailedBatch(), JobLogger.KEY_PROCESSED to service.drain(), "outstanding" to service.outstandingCounts())
        }
    }
}
