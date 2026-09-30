package com.peakda.server.infrastructure.scheduler.kto

import com.peakda.server.domain.congestion.application.CongestionAttractionLinkService
import com.peakda.server.domain.congestion.application.CongestionLinkSummary
import com.peakda.server.infrastructure.scheduler.JobLogger
import com.peakda.server.infrastructure.scheduler.ManualTriggerableJob
import com.peakda.server.infrastructure.scheduler.SchedulerProperties
import com.peakda.server.infrastructure.scheduler.SchedulerTime.KST
import com.peakda.server.infrastructure.scheduler.SchedulerTime.YMD
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * 집중률 관광지명을 명소에 연결한다. 집중률 동기화([TatsCnctrSyncJob]) 이후에 돈다.
 *
 * 확정·거절된 연결은 건드리지 않고, 검토 대기·후보 없음만 다시 평가한다(새 명소가 적재됐을 수 있다).
 */
@Component
class CongestionAttractionLinkJob(
    private val linkService: CongestionAttractionLinkService,
    private val props: SchedulerProperties,
    private val jobLogger: JobLogger,
) : ManualTriggerableJob {
    override val jobName: String
        get() = JOB_NAME

    @Scheduled(cron = "\${external.scheduler.kto.congestion-link.cron}", zone = "Asia/Seoul")
    fun run() {
        jobLogger.runIfEnabled(JOB_NAME, props.enabled && props.kto.congestionLink.enabled) { execute() }
    }

    override fun runNow() {
        jobLogger.runManually(JOB_NAME) { execute() }
    }

    private fun execute(): Map<String, Any?> {
        val today = LocalDate.now(KST).format(YMD)
        val targets = linkService.findTargetsBySigungu(today)
        val summary = targets.entries.fold(CongestionLinkSummary()) { acc, (region, names) ->
            acc + linkService.linkSigungu(region.first, region.second, names)
        }
        return mapOf(
            JobLogger.KEY_PROCESSED to summary.evaluated,
            JobLogger.KEY_TOTAL to targets.values.sumOf { it.size },
            "sigungu" to targets.size,
            "skipped" to summary.skipped,
        ) + summary.byStatus.mapKeys { (status, _) -> status.name.lowercase() }
    }

    companion object {
        const val JOB_NAME = "congestionAttractionLink"
    }
}
