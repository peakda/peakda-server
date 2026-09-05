package com.peakda.server.infrastructure.scheduler.kto

import com.peakda.server.domain.gallery.application.GalleryPhotoSyncService
import com.peakda.server.infrastructure.external.kto.photo.PhotoGalleryClient
import com.peakda.server.infrastructure.scheduler.JobLogger
import com.peakda.server.infrastructure.scheduler.ManualTriggerableJob
import com.peakda.server.infrastructure.scheduler.SchedulerProperties
import com.peakda.server.infrastructure.scheduler.runPaging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class PhotoGallerySyncJob(
    private val client: PhotoGalleryClient,
    private val syncService: GalleryPhotoSyncService,
    private val props: SchedulerProperties,
    private val jobLogger: JobLogger,
) : ManualTriggerableJob {
    override val jobName: String
        get() = JOB_NAME

    @Scheduled(cron = "\${external.scheduler.kto.photo.cron}", zone = "Asia/Seoul")
    fun run() {
        jobLogger.runIfEnabled(JOB_NAME, props.enabled && props.kto.photo.enabled) { execute() }
    }

    override fun runNow() {
        jobLogger.runManually(JOB_NAME) { execute() }
    }

    private fun execute(): Map<String, Any?> {
        val result = runPaging(fetch = client::galleryList, upsert = syncService::upsertPage)
        return mapOf(
            JobLogger.KEY_PROCESSED to result.processed,
            JobLogger.KEY_TOTAL to result.totalCount,
        )
    }

    companion object {
        const val JOB_NAME = "photoGallerySync"
    }
}
