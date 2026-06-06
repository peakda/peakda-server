package com.peakda.server.infrastructure.scheduler.seasonal

import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.seasonal.application.BloomTaggingService
import com.peakda.server.infrastructure.scheduler.JobLogger
import com.peakda.server.infrastructure.scheduler.SchedulerProperties
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneId

/**
 * 명소 ↔ 꽃·계절 카테고리 자동 태깅 잡. 외부 API 호출이 없는 순수 내부 산출 잡.
 *
 * 신호 A(키워드)는 visible 명소를 페이지 단위로 스캔하며, 페이지마다 별도 트랜잭션으로 커밋한다.
 * 신호 B(축제)는 활성 축제를 근접 명소에 매칭한다.
 */
@Component
class AttractionBloomTaggingJob(
    private val taggingService: BloomTaggingService,
    private val attractionRepository: AttractionRepository,
    private val props: SchedulerProperties,
    private val jobLogger: JobLogger,
) {
    @Scheduled(cron = "\${external.scheduler.seasonal.attraction-bloom-tagging.cron}", zone = "Asia/Seoul")
    fun run() {
        jobLogger.runIfEnabled(JOB_NAME, props.enabled && props.seasonal.attractionBloomTagging.enabled) {
            var page = 0
            var processedAttractions = 0
            var keywordTags = 0
            while (true) {
                val slice = attractionRepository.findByVisibleTrue(PageRequest.of(page, PAGE_SIZE))
                if (slice.isEmpty) break
                keywordTags += taggingService.tagKeywords(slice.content)
                processedAttractions += slice.numberOfElements
                if (!slice.hasNext()) break
                page++
            }
            val festivalTags = taggingService.tagFestivals(LocalDate.now(KST))
            mapOf(
                JobLogger.KEY_PROCESSED to keywordTags + festivalTags,
                "attractions" to processedAttractions,
                "keywordTags" to keywordTags,
                "festivalTags" to festivalTags,
            )
        }
    }

    companion object {
        const val JOB_NAME = "attractionBloomTagging"
        private const val PAGE_SIZE = 500
        private val KST = ZoneId.of("Asia/Seoul")
    }
}
