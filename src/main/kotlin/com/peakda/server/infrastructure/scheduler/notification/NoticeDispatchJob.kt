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

    /**
     * 30초 폴링이라 발송할 공지가 없을 때까지 실행 이력을 남기면 `scheduler_job_runs` 가 처리 0건 행으로 뒤덮인다.
     * 발송 대기 공지가 있을 때만 락·이력 경로로 들어간다. 수동 실행([runNow])은 항상 이력을 남긴다.
     */
    @Scheduled(fixedDelayString = "\${external.scheduler.notification.notice-dispatch.fixed-delay}")
    fun run() {
        val enabled = props.enabled && props.notification.noticeDispatch.enabled
        if (enabled && !service.hasPendingDispatch()) return
        jobLogger.runIfEnabled(JOB_NAME, enabled) { execute() }
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
