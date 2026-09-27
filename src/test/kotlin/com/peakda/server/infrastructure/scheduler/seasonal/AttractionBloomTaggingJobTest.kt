package com.peakda.server.infrastructure.scheduler.seasonal

import com.peakda.server.domain.attraction.application.AttractionEligibilityProperties
import com.peakda.server.domain.attraction.entity.Attraction
import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.seasonal.application.BloomTaggingService
import com.peakda.server.infrastructure.scheduler.SchedulerProperties
import com.peakda.server.infrastructure.scheduler.testJobLogger
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.springframework.data.domain.SliceImpl
import java.time.Instant

class AttractionBloomTaggingJobTest {
    private val attraction = Attraction(
        tourApiContentId = "tourist-1",
        contentTypeCode = "12",
        title = "벚꽃 명소",
    )
    private var keywordTags = 1
    private val taggingService = mock(BloomTaggingService::class.java) { invocation ->
        when (invocation.method.name) {
            "tagKeywords" -> keywordTags
            "tagFestivals", "deleteStaleAutoTags" -> 0
            else -> null
        }
    }
    private val attractionRepository = mock(AttractionRepository::class.java) { invocation ->
        if (invocation.method.name == "findByVisibleTrueAndContentTypeCodeIn") SliceImpl(listOf(attraction)) else null
    }
    private val job = AttractionBloomTaggingJob(
        taggingService,
        attractionRepository,
        AttractionEligibilityProperties(setOf("12")),
        SchedulerProperties(),
        testJobLogger(),
    )

    @Test
    fun `키워드 태깅은 서비스 대상 유형의 visible 명소만 조회한다`() {
        job.runNow()

        val lookup = mockingDetails(attractionRepository).invocations.single()
        assertThat(lookup.method.name).isEqualTo("findByVisibleTrueAndContentTypeCodeIn")
        assertThat(lookup.arguments[0]).isEqualTo(setOf("12"))
        val tagged = mockingDetails(taggingService).invocations.single { it.method.name == "tagKeywords" }
        assertThat(tagged.arguments[0]).isEqualTo(listOf(attraction))
    }

    @Test
    fun `태그를 만들었으면 실행 시작 시각 기준으로 오래된 자동 태그를 정리한다`() {
        val before = Instant.now()

        job.runNow()

        val cleanup = mockingDetails(taggingService).invocations.single { it.method.name == "deleteStaleAutoTags" }
        assertThat(cleanup.arguments[0] as Instant).isBetween(before, Instant.now())
    }

    @Test
    fun `만든 태그가 없으면 정리를 건너뛴다`() {
        keywordTags = 0

        job.runNow()

        assertThat(mockingDetails(taggingService).invocations.map { it.method.name })
            .doesNotContain("deleteStaleAutoTags")
    }
}
