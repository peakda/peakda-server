package com.peakda.server.infrastructure.scheduler.kto

import com.peakda.server.domain.visitor.application.RegionVisitorSyncService
import com.peakda.server.infrastructure.external.kto.datalab.DataLabClient
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
class DataLabSyncJob(
    private val client: DataLabClient,
    private val syncService: RegionVisitorSyncService,
    private val props: SchedulerProperties,
    private val jobLogger: JobLogger,
) : ManualTriggerableJob {
    override val jobName: String
        get() = JOB_NAME

    @Scheduled(cron = "\${external.scheduler.kto.data-lab.cron}", zone = "Asia/Seoul")
    fun run() {
        jobLogger.runIfEnabled(JOB_NAME, props.enabled && props.kto.dataLab.enabled) { execute() }
    }

    override fun runNow() {
        jobLogger.runManually(JOB_NAME) { execute() }
    }

    private fun execute(): Map<String, Any?> {
        val today = LocalDate.now(KST)
        val endYmd = today.minusDays(1).format(YMD)
        val startYmd = today.minusDays(BACKFILL_DAYS).format(YMD)
        val result = runPaging(
            extras = mapOf("startYmd" to startYmd, "endYmd" to endYmd),
            fetch = client::metcoRegnVisitrDDList,
            upsert = syncService::upsertPage,
        )
        return mapOf(
            JobLogger.KEY_PROCESSED to result.processed,
            JobLogger.KEY_TOTAL to result.totalCount,
            "startYmd" to startYmd,
            "endYmd" to endYmd,
        )
    }

    companion object {
        const val JOB_NAME = "dataLabSync"
        private const val BACKFILL_DAYS = 7L
    }
}
