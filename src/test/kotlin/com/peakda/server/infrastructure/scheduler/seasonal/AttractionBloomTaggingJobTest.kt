package com.peakda.server.infrastructure.scheduler.seasonal

import com.peakda.server.domain.attraction.application.AttractionEligibilityProperties
import com.peakda.server.domain.attraction.entity.Attraction
import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.seasonal.application.BloomTaggingService
import com.peakda.server.domain.seasonal.entity.TagSource
import com.peakda.server.infrastructure.scheduler.SchedulerProperties
import com.peakda.server.infrastructure.scheduler.testJobLogger
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.SliceImpl
import org.springframework.data.domain.Sort
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
    fun `키워드 태깅은 서비스 대상 유형의 visible 명소를 id 순으로 조회한다`() {
        job.runNow()

        val lookup = mockingDetails(attractionRepository).invocations.single()
        assertThat(lookup.method.name).isEqualTo("findByVisibleTrueAndContentTypeCodeIn")
        assertThat(lookup.arguments[0]).isEqualTo(setOf("12"))
        assertThat((lookup.arguments[1] as Pageable).sort).isEqualTo(Sort.by(Sort.Direction.ASC, "id"))
        val tagged = mockingDetails(taggingService).invocations.single { it.method.name == "tagKeywords" }
        assertThat(tagged.arguments[0]).isEqualTo(listOf(attraction))
    }

    @Test
    fun `태그를 만든 출처만 실행 시작 시각 기준으로 정리한다`() {
        val before = Instant.now()

        job.runNow()

        val cleanup = mockingDetails(taggingService).invocations.single { it.method.name == "deleteStaleAutoTags" }
        assertThat(cleanup.arguments[0] as Instant).isBetween(before, Instant.now())
        assertThat(cleanup.arguments[1]).isEqualTo(setOf(TagSource.KEYWORD))
    }

    @Test
    fun `만든 태그가 없으면 어느 출처도 정리하지 않는다`() {
        keywordTags = 0

        job.runNow()

        val cleanup = mockingDetails(taggingService).invocations.single { it.method.name == "deleteStaleAutoTags" }
        assertThat(cleanup.arguments[1]).isEqualTo(emptySet<TagSource>())
    }
}
