package com.peakda.server.infrastructure.scheduler.notification

import com.peakda.server.domain.notification.application.BloomTimingAlertService
import com.peakda.server.infrastructure.scheduler.JobLogger
import com.peakda.server.infrastructure.scheduler.ManualTriggerableJob
import com.peakda.server.infrastructure.scheduler.SchedulerProperties
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneId

@Component
class BloomTimingAlertJob(
    private val service: BloomTimingAlertService,
    private val props: SchedulerProperties,
    private val jobLogger: JobLogger,
) : ManualTriggerableJob {
    override val jobName: String
        get() = JOB_NAME

    @Scheduled(cron = "\${external.scheduler.notification.bloom-timing-alert.cron}", zone = "Asia/Seoul")
    fun run() {
        jobLogger.runIfEnabled(JOB_NAME, props.enabled && props.notification.bloomTimingAlert.enabled) { execute() }
    }

    override fun runNow() {
        jobLogger.runManually(JOB_NAME) { execute() }
    }

    private fun execute(): Map<String, Any?> {
        val sent = service.sendDueAlerts(LocalDate.now(KST))
        return mapOf(JobLogger.KEY_PROCESSED to sent)
    }

    companion object {
        const val JOB_NAME = "bloomTimingAlert"
        private val KST = ZoneId.of("Asia/Seoul")
    }
}
