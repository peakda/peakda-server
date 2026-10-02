package com.peakda.server.infrastructure.scheduler.kto

import com.peakda.server.domain.gallery.application.GalleryPhotoSyncService
import com.peakda.server.infrastructure.external.kto.photo.PhotoGalleryClient
import com.peakda.server.infrastructure.scheduler.JobLogger
import com.peakda.server.infrastructure.scheduler.ManualTriggerableJob
import com.peakda.server.infrastructure.scheduler.SchedulerProperties
import com.peakda.server.infrastructure.scheduler.runPaging
import org.slf4j.LoggerFactory
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
        val result = runPaging(
            pageSize = PAGE_SIZE,
            maxPages = MAX_PAGES,
            fetch = client::galleryList,
            upsert = syncService::upsertPage,
        )
        if (result.processed < result.totalCount) {
            log.warn("[photoGallerySync] stopped early processed={} total={}", result.processed, result.totalCount)
        }
        return mapOf(
            JobLogger.KEY_PROCESSED to result.processed,
            JobLogger.KEY_TOTAL to result.totalCount,
        )
    }

    companion object {
        const val JOB_NAME = "photoGallerySync"
        private const val PAGE_SIZE = 1000

        /**
         * 공용 기본값(100건 × 50페이지)은 5,000건에서 잘려 2023-11 이후 사진을 받지 못했다.
         * 서버가 페이지를 100건으로 잘라도 2만 건까지는 받도록 둔다.
         */
        private const val MAX_PAGES = 200
        private val log = LoggerFactory.getLogger(PhotoGallerySyncJob::class.java)
    }
}
