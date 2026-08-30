package com.peakda.server.infrastructure.scheduler.notification

import com.peakda.server.domain.notification.application.DeviceTokenService
import com.peakda.server.infrastructure.scheduler.JobLogger
import com.peakda.server.infrastructure.scheduler.ManualTriggerableJob
import com.peakda.server.infrastructure.scheduler.SchedulerProperties
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class DeviceTokenCleanupJob(
    private val service: DeviceTokenService,
    private val props: SchedulerProperties,
    private val jobLogger: JobLogger,
) : ManualTriggerableJob {
    override val jobName: String
        get() = JOB_NAME

    @Scheduled(cron = "\${external.scheduler.notification.device-token-cleanup.cron}", zone = "Asia/Seoul")
    fun run() {
        jobLogger.runIfEnabled(JOB_NAME, props.enabled && props.notification.deviceTokenCleanup.enabled) { execute() }
    }

    override fun runNow() {
        jobLogger.runManually(JOB_NAME) { execute() }
    }

    private fun execute(): Map<String, Any?> =
        mapOf(JobLogger.KEY_PROCESSED to service.deleteStale())

    companion object {
        const val JOB_NAME = "deviceTokenCleanup"
    }
}
