package com.peakda.server.infrastructure.scheduler.seasonal

import com.peakda.server.domain.attraction.application.AttractionEligibilityProperties
import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.seasonal.application.BloomTaggingService
import com.peakda.server.domain.seasonal.entity.TagSource
import com.peakda.server.infrastructure.scheduler.JobLogger
import com.peakda.server.infrastructure.scheduler.ManualTriggerableJob
import com.peakda.server.infrastructure.scheduler.SchedulerProperties
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 명소 ↔ 꽃·계절 카테고리 자동 태깅 잡. 외부 API 호출이 없는 순수 내부 산출 잡.
 *
 * 신호 A(키워드)는 visible 명소를 페이지 단위로 스캔하며, 페이지마다 별도 트랜잭션으로 커밋한다.
 * 신호 B(축제)는 활성 축제를 장소명이 일치하는 명소에 매칭한다.
 * 두 신호가 끝나면 이번 실행에서 갱신되지 않은 자동 태그를 출처별로 정리한다. 한 출처의 태그가 하나도 만들어지지 않았으면
 * (데이터 공백·장애 의심) 그 출처는 정리를 건너뛴다. 정리가 누락을 곧 삭제로 만들므로 키워드 순회는 id 순으로 고정한다.
 */
@Component
class AttractionBloomTaggingJob(
    private val taggingService: BloomTaggingService,
    private val attractionRepository: AttractionRepository,
    private val eligibilityProperties: AttractionEligibilityProperties,
    private val props: SchedulerProperties,
    private val jobLogger: JobLogger,
) : ManualTriggerableJob {
    override val jobName: String
        get() = JOB_NAME

    @Scheduled(cron = "\${external.scheduler.seasonal.attraction-bloom-tagging.cron}", zone = "Asia/Seoul")
    fun run() {
        jobLogger.runIfEnabled(JOB_NAME, props.enabled && props.seasonal.attractionBloomTagging.enabled) { execute() }
    }

    override fun runNow() {
        jobLogger.runManually(JOB_NAME) { execute() }
    }

    private fun execute(): Map<String, Any?> {
        val runStartedAt = Instant.now()
        var page = 0
        var processedAttractions = 0
        var keywordTags = 0
        var categoryTags = 0
        while (true) {
            val slice = attractionRepository.findByVisibleTrueAndContentTypeCodeIn(
                eligibilityProperties.eligibleContentTypes,
                PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.ASC, "id")),
            )
            if (slice.isEmpty) break
            keywordTags += taggingService.tagKeywords(slice.content)
            categoryTags += taggingService.tagCategories(slice.content)
            processedAttractions += slice.numberOfElements
            if (!slice.hasNext()) break
            page++
        }
        val festivalTags = taggingService.tagFestivals(LocalDate.now(KST))
        val cleanupSources = buildSet {
            if (keywordTags > 0) add(TagSource.KEYWORD)
            if (festivalTags > 0) add(TagSource.FESTIVAL)
            if (categoryTags > 0) add(TagSource.CATEGORY)
        }
        val staleDeleted = taggingService.deleteStaleAutoTags(runStartedAt, cleanupSources)
        return mapOf(
            JobLogger.KEY_PROCESSED to keywordTags + festivalTags + categoryTags,
            "attractions" to processedAttractions,
            "keywordTags" to keywordTags,
            "festivalTags" to festivalTags,
            "categoryTags" to categoryTags,
            "staleDeleted" to staleDeleted,
            "staleCleanedSources" to cleanupSources.map(TagSource::name),
        )
    }

    companion object {
        const val JOB_NAME = "attractionBloomTagging"
        private const val PAGE_SIZE = 500
        private val KST = ZoneId.of("Asia/Seoul")
    }
}
