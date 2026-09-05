package com.peakda.server.infrastructure.scheduler.seasonal

import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.seasonal.application.AttractionStationMappingService
import com.peakda.server.infrastructure.scheduler.JobLogger
import com.peakda.server.infrastructure.scheduler.ManualTriggerableJob
import com.peakda.server.infrastructure.scheduler.SchedulerProperties
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/** 명소 좌표 변경과 신규 명소를 반복 실행 가능한 upsert로 최근접 ASOS 지점에 매핑한다. */
@Component
class AttractionStationMappingJob(
    private val mappingService: AttractionStationMappingService,
    private val attractionRepository: AttractionRepository,
    private val props: SchedulerProperties,
    private val jobLogger: JobLogger,
) : ManualTriggerableJob {
    override val jobName: String
        get() = JOB_NAME

    @Scheduled(cron = "\${external.scheduler.seasonal.attraction-station-mapping.cron}", zone = "Asia/Seoul")
    fun run() {
        jobLogger.runIfEnabled(JOB_NAME, props.enabled && props.seasonal.attractionStationMapping.enabled) { execute() }
    }

    override fun runNow() {
        jobLogger.runManually(JOB_NAME) { execute() }
    }

    private fun execute(): Map<String, Any?> {
        var page = 0
        var processed = 0
        while (true) {
            val slice = attractionRepository.findByVisibleTrue(PageRequest.of(page, PAGE_SIZE))
            if (slice.isEmpty) break
            processed += mappingService.mapPage(slice.content)
            if (!slice.hasNext()) break
            page++
        }
        return mapOf(JobLogger.KEY_PROCESSED to processed)
    }

    companion object {
        const val JOB_NAME = "attractionStationMapping"
        private const val PAGE_SIZE = 500
    }
}
