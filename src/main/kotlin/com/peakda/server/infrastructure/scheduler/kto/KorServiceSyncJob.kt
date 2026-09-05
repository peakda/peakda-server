package com.peakda.server.infrastructure.scheduler.kto

import com.peakda.server.domain.attraction.application.AttractionSyncService
import com.peakda.server.domain.spot.application.AttractionSpotMaterializationService
import com.peakda.server.infrastructure.external.kto.korservice.KorServiceClient
import com.peakda.server.infrastructure.scheduler.JobLogger
import com.peakda.server.infrastructure.scheduler.ManualTriggerableJob
import com.peakda.server.infrastructure.scheduler.SchedulerProperties
import com.peakda.server.infrastructure.scheduler.SchedulerTime.KST
import com.peakda.server.infrastructure.scheduler.SchedulerTime.YMD
import com.peakda.server.infrastructure.scheduler.runPaging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class KorServiceSyncJob(
    private val client: KorServiceClient,
    private val syncService: AttractionSyncService,
    private val materializationService: AttractionSpotMaterializationService,
    private val props: SchedulerProperties,
    private val jobLogger: JobLogger,
) : ManualTriggerableJob {
    override val jobName: String
        get() = JOB_NAME

    @Scheduled(cron = "\${external.scheduler.kto.kor-service.cron}", zone = "Asia/Seoul")
    fun run() {
        jobLogger.runIfEnabled(JOB_NAME, props.enabled && props.kto.korService.enabled) { execute() }
    }

    override fun runNow() {
        jobLogger.runManually(JOB_NAME) { execute() }
    }

    private fun execute(): Map<String, Any?> {
        val modifiedTime = LocalDate.now(KST).minusDays(1).format(YMD)
        val result = runPaging(
            extras = mapOf("modifiedtime" to modifiedTime),
            fetch = client::areaBasedSyncList,
            upsert = syncService::upsertPage,
        )
        val materialization = materializationService.materializeVisibleAttractions()
        return mapOf(
            JobLogger.KEY_PROCESSED to result.processed + materialization.processed,
            JobLogger.KEY_TOTAL to result.totalCount,
            "modifiedtime" to modifiedTime,
            "spotProcessed" to materialization.processed,
            "spotSkippedNoCoordinates" to materialization.skippedNoCoordinates,
        )
    }

    companion object {
        const val JOB_NAME = "korServiceSync"
    }
}
