package com.peakda.server.infrastructure.scheduler.kto

import com.peakda.server.domain.trail.application.WalkingCourseSyncService
import com.peakda.server.domain.trail.application.WalkingRouteSyncService
import com.peakda.server.infrastructure.external.kto.durunubi.DurunubiClient
import com.peakda.server.infrastructure.scheduler.JobLogger
import com.peakda.server.infrastructure.scheduler.ManualTriggerableJob
import com.peakda.server.infrastructure.scheduler.SchedulerProperties
import com.peakda.server.infrastructure.scheduler.runPaging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class DurunubiSyncJob(
    private val client: DurunubiClient,
    private val routeSyncService: WalkingRouteSyncService,
    private val courseSyncService: WalkingCourseSyncService,
    private val props: SchedulerProperties,
    private val jobLogger: JobLogger,
) : ManualTriggerableJob {
    override val jobName: String
        get() = JOB_NAME

    @Scheduled(cron = "\${external.scheduler.kto.durunubi.cron}", zone = "Asia/Seoul")
    fun run() {
        jobLogger.runIfEnabled(JOB_NAME, props.enabled && props.kto.durunubi.enabled) { execute() }
    }

    override fun runNow() {
        jobLogger.runManually(JOB_NAME) { execute() }
    }

    private fun execute(): Map<String, Any?> {
        val routes = runPaging(fetch = client::routeList, upsert = routeSyncService::upsertPage).processed
        val courses = runPaging(fetch = client::courseList, upsert = courseSyncService::upsertPage).processed
        return mapOf(
            JobLogger.KEY_PROCESSED to routes + courses,
            "routes" to routes,
            "courses" to courses,
        )
    }

    companion object {
        const val JOB_NAME = "durunubiSync"
    }
}
