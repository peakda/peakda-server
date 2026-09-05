package com.peakda.server.infrastructure.scheduler.notification

import com.peakda.server.domain.notification.application.NoticeFanoutService
import com.peakda.server.infrastructure.scheduler.JobLogger
import com.peakda.server.infrastructure.scheduler.ManualTriggerableJob
import com.peakda.server.infrastructure.scheduler.SchedulerProperties
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class NoticeDispatchJob(
    private val service: NoticeFanoutService,
    private val props: SchedulerProperties,
    private val jobLogger: JobLogger,
) : ManualTriggerableJob {
    override val jobName: String
        get() = JOB_NAME

    @Scheduled(fixedDelayString = "\${external.scheduler.notification.notice-dispatch.fixed-delay}")
    fun run() {
        jobLogger.runIfEnabled(JOB_NAME, props.enabled && props.notification.noticeDispatch.enabled) { execute() }
    }

    override fun runNow() {
        jobLogger.runManually(JOB_NAME) { execute() }
    }

    private fun execute(): Map<String, Any?> =
        mapOf(JobLogger.KEY_PROCESSED to service.dispatchPending())

    companion object {
        const val JOB_NAME = "noticeDispatch"
    }
}
