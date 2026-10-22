package com.peakda.server.infrastructure.scheduler.launchalert

import com.peakda.server.domain.launchalert.application.LaunchAlertFanoutService
import com.peakda.server.infrastructure.scheduler.JobLogger
import com.peakda.server.infrastructure.scheduler.ManualTriggerableJob
import com.peakda.server.infrastructure.scheduler.SchedulerProperties
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/** 관리자가 요청한 출시 안내 메일을 30초마다 한 묶음씩 보낸다. */
@Component
class LaunchAlertDispatchJob(
    private val service: LaunchAlertFanoutService,
    private val props: SchedulerProperties,
    private val jobLogger: JobLogger,
) : ManualTriggerableJob {
    override val jobName: String
        get() = JOB_NAME

    /** 발송 요청이 없을 때는 락·실행 이력 경로로 들어가지 않는다. 처리 0건 이력이 쌓이지 않게 한다. */
    @Scheduled(fixedDelayString = "\${external.scheduler.notification.launch-alert-dispatch.fixed-delay}")
    fun run() {
        val enabled = props.enabled && props.notification.launchAlertDispatch.enabled
        if (enabled && !service.hasPendingDispatch()) return
        jobLogger.runIfEnabled(JOB_NAME, enabled) { execute() }
    }

    override fun runNow() {
        jobLogger.runManually(JOB_NAME) { execute() }
    }

    private fun execute(): Map<String, Any?> =
        mapOf(JobLogger.KEY_PROCESSED to service.dispatchNextBatch())

    companion object {
        const val JOB_NAME = "launchAlertDispatch"
    }
}
