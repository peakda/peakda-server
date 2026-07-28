package com.peakda.server.infrastructure.scheduler.seasonal

import com.peakda.server.domain.festival.repository.FestivalRepository
import com.peakda.server.domain.seasonal.application.BloomEstimateService
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.repository.AttractionBloomRepository
import com.peakda.server.infrastructure.scheduler.JobLogger
import com.peakda.server.infrastructure.scheduler.ManualTriggerableJob
import com.peakda.server.infrastructure.scheduler.SchedulerProperties
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneId

/**
 * 명소×카테고리 개화 상태 추정 잡. 태깅된 [AttractionBloom] 을 입력으로 추정기·융합을 돌려 산출일(today) 기준 상태를 적재한다.
 *
 * 좌표 보유 축제를 한 번만 로드해 모든 추정에 공유하고, 카테고리별로 명소 id 를 페이지 단위로 순회하며 페이지마다 커밋한다.
 */
@Component
class BloomEstimateJob(
    private val attractionBloomRepository: AttractionBloomRepository,
    private val festivalRepository: FestivalRepository,
    private val estimateService: BloomEstimateService,
    private val props: SchedulerProperties,
    private val jobLogger: JobLogger,
) : ManualTriggerableJob {
    override val jobName: String
        get() = JOB_NAME

    @Scheduled(cron = "\${external.scheduler.seasonal.bloom-estimate.cron}", zone = "Asia/Seoul")
    fun run() {
        jobLogger.runIfEnabled(JOB_NAME, props.enabled && props.seasonal.bloomEstimate.enabled) { execute() }
    }

    override fun runNow() {
        jobLogger.runManually(JOB_NAME) { execute() }
    }

    private fun execute(): Map<String, Any?> {
        val today = LocalDate.now(KST)
        val festivals = festivalRepository.findByLatitudeIsNotNullAndLongitudeIsNotNull()
        var estimates = 0
        for (category in BloomCategory.entries) {
            var page = 0
            while (true) {
                val slice = attractionBloomRepository
                    .findDistinctAttractionIdsByBloomCategory(category, PageRequest.of(page, PAGE_SIZE))
                if (slice.isEmpty) break
                estimates += estimateService.estimatePage(slice.content, category, today, festivals)
                if (!slice.hasNext()) break
                page++
            }
        }
        return mapOf(
            JobLogger.KEY_PROCESSED to estimates,
            "festivals" to festivals.size,
        )
    }

    companion object {
        const val JOB_NAME = "bloomEstimate"
        private const val PAGE_SIZE = 500
        private val KST = ZoneId.of("Asia/Seoul")
    }
}
