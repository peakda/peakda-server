package com.peakda.server.infrastructure.scheduler.kto

import com.peakda.server.domain.congestion.application.CongestionSyncService
import com.peakda.server.infrastructure.external.kto.tatscnctr.TatsCnctrClient
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
class TatsCnctrSyncJob(
    private val client: TatsCnctrClient,
    private val syncService: CongestionSyncService,
    private val props: SchedulerProperties,
    private val jobLogger: JobLogger,
) : ManualTriggerableJob {
    override val jobName: String
        get() = JOB_NAME

    @Scheduled(cron = "\${external.scheduler.kto.tats-cnctr.cron}", zone = "Asia/Seoul")
    fun run() {
        jobLogger.runIfEnabled(JOB_NAME, props.enabled && props.kto.tatsCnctr.enabled) { execute() }
    }

    override fun runNow() {
        jobLogger.runManually(JOB_NAME) { execute() }
    }

    private fun execute(): Map<String, Any?> {
        val baseYmd = LocalDate.now(KST).format(YMD)
        val result = runPaging(
            extras = mapOf("baseYmd" to baseYmd),
            fetch = client::tatsCnctrRateList,
            upsert = syncService::upsertPage,
        )
        return mapOf(
            JobLogger.KEY_PROCESSED to result.processed,
            JobLogger.KEY_TOTAL to result.totalCount,
            "baseYmd" to baseYmd,
        )
    }

    companion object {
        const val JOB_NAME = "tatsCnctrSync"
    }
}
