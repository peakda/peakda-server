package com.peakda.server.infrastructure.scheduler.kma

import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.seasonal.repository.AttractionBloomRepository
import com.peakda.server.domain.weather.application.AttractionForecastAreaMappingService
import com.peakda.server.infrastructure.scheduler.JobLogger
import com.peakda.server.infrastructure.scheduler.ManualTriggerableJob
import com.peakda.server.infrastructure.scheduler.SchedulerProperties
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 개화 태그가 있는 명소를 기상청 예보 구역(단기 격자·중기 구역)에 매핑한다.
 *
 * 모든 명소를 매핑하면 단기예보 수집 격자가 호출 한도를 넘으므로 계절 명소로 한정한다.
 * 꽃 태깅(05:45) 이후, 단기예보 수집 전에 돈다.
 */
@Component
class AttractionForecastAreaMappingJob(
    private val mappingService: AttractionForecastAreaMappingService,
    private val attractionBloomRepository: AttractionBloomRepository,
    private val attractionRepository: AttractionRepository,
    private val props: SchedulerProperties,
    private val jobLogger: JobLogger,
) : ManualTriggerableJob {
    override val jobName: String
        get() = JOB_NAME

    @Scheduled(cron = "\${external.scheduler.kma.forecast-area-mapping.cron}", zone = "Asia/Seoul")
    fun run() {
        jobLogger.runIfEnabled(JOB_NAME, props.enabled && props.kma.forecastAreaMapping.enabled) { execute() }
    }

    override fun runNow() {
        jobLogger.runManually(JOB_NAME) { execute() }
    }

    private fun execute(): Map<String, Any?> {
        var page = 0
        var processed = 0
        var total = 0
        while (true) {
            val ids = attractionBloomRepository.findDistinctAttractionIds(PageRequest.of(page, PAGE_SIZE))
            if (ids.isEmpty) break
            total += ids.numberOfElements
            processed += mappingService.mapPage(attractionRepository.findAllById(ids.content))
            if (!ids.hasNext()) break
            page++
        }
        return mapOf(JobLogger.KEY_PROCESSED to processed, JobLogger.KEY_TOTAL to total)
    }

    companion object {
        const val JOB_NAME = "attractionForecastAreaMapping"
        private const val PAGE_SIZE = 500
    }
}
