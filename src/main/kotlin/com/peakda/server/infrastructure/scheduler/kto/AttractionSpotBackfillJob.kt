package com.peakda.server.infrastructure.scheduler.kto

import com.peakda.server.domain.spot.application.AttractionSpotMaterializationService
import com.peakda.server.infrastructure.scheduler.JobLogger
import com.peakda.server.infrastructure.scheduler.ManualTriggerableJob
import org.springframework.stereotype.Component

/** 관리자 수동 트리거로 서비스 대상 명소의 명소형 Spot을 일괄 생성하고 명소형 Spot 공개 여부를 명소 상태에 맞춘다. */
@Component
class AttractionSpotBackfillJob(
    private val materializationService: AttractionSpotMaterializationService,
    private val jobLogger: JobLogger,
) : ManualTriggerableJob {
    override val jobName: String
        get() = JOB_NAME

    override fun runNow() {
        jobLogger.runManually(JOB_NAME) { execute() }
    }

    private fun execute(): Map<String, Any?> {
        val result = materializationService.materializeVisibleAttractions()
        return mapOf(
            JobLogger.KEY_PROCESSED to result.processed,
            "skippedNoCoordinates" to result.skippedNoCoordinates,
            "pages" to result.pages,
            "hidden" to result.hidden,
            "shown" to result.shown,
        )
    }

    companion object {
        const val JOB_NAME = "attractionSpotBackfill"
    }
}
