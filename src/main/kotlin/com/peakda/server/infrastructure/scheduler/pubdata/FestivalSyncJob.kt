package com.peakda.server.infrastructure.scheduler.pubdata

import com.peakda.server.domain.festival.application.FestivalSyncService
import com.peakda.server.infrastructure.external.pubdata.festival.FestivalClient
import com.peakda.server.infrastructure.scheduler.JobLogger
import com.peakda.server.infrastructure.scheduler.ManualTriggerableJob
import com.peakda.server.infrastructure.scheduler.SchedulerProperties
import com.peakda.server.infrastructure.scheduler.runPaging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class FestivalSyncJob(
    private val client: FestivalClient,
    private val syncService: FestivalSyncService,
    private val props: SchedulerProperties,
    private val jobLogger: JobLogger,
) : ManualTriggerableJob {
    override val jobName: String
        get() = JOB_NAME

    @Scheduled(cron = "\${external.scheduler.pubdata.festival.cron}", zone = "Asia/Seoul")
    fun run() {
        jobLogger.runIfEnabled(JOB_NAME, props.enabled && props.pubdata.festival.enabled) { execute() }
    }

    override fun runNow() {
        jobLogger.runManually(JOB_NAME) { execute() }
    }

    private fun execute(): Map<String, Any?> {
        val result = runPaging(maxPages = 100, fetch = client::list, upsert = syncService::upsertPage)
        return mapOf(
            JobLogger.KEY_PROCESSED to result.processed,
            JobLogger.KEY_TOTAL to result.totalCount,
        )
    }

    companion object {
        const val JOB_NAME = "festivalSync"
    }
}
