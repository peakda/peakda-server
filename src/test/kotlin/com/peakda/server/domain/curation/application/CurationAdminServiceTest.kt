package com.peakda.server.domain.curation.application

import com.peakda.server.domain.curation.entity.Curation
import com.peakda.server.domain.curation.entity.CurationChapter
import com.peakda.server.domain.curation.entity.CurationLayout
import com.peakda.server.domain.curation.entity.CurationRecommendation
import com.peakda.server.domain.curation.entity.CurationStatus
import com.peakda.server.domain.curation.repository.CurationChapterRepository
import com.peakda.server.domain.curation.repository.CurationRecommendationRepository
import com.peakda.server.domain.curation.repository.CurationRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.test.util.ReflectionTestUtils
import java.time.Instant
import java.time.LocalDate
import java.util.Optional

class CurationAdminServiceTest {

    private val curationRepository = mock(CurationRepository::class.java)
    private val curationChapterRepository = mock(CurationChapterRepository::class.java)
    private val curationRecommendationRepository = mock(CurationRecommendationRepository::class.java)
    private val service = CurationAdminService(
        curationRepository,
        curationChapterRepository,
        curationRecommendationRepository,
    )

    @Test
    fun `같은 주차를 두 번 저장하면 새 행 없이 갱신하고 하위 항목을 전량 교체한다`() {
        val persisted = curation(CURATION_ID, CurationStatus.DRAFT, title = "첫 제목")
        `when`(curationRepository.findByWeekStartDate(WEEK_START)).thenReturn(null, persisted)
        `when`(curationRepository.save(anyCuration())).thenReturn(persisted)

        val firstId = service.upsert(command(title = "첫 제목"))
        val secondId = service.upsert(command(title = "수정 제목"))

        assertThat(firstId).isEqualTo(CURATION_ID)
        assertThat(secondId).isEqualTo(CURATION_ID)
        assertThat(persisted.title).isEqualTo("수정 제목")
        verify(curationRepository, times(1)).save(anyCuration())
        verify(curationChapterRepository, times(2)).deleteByCurationId(CURATION_ID)
        verify(curationRecommendationRepository, times(2)).deleteByCurationId(CURATION_ID)
    }

    @Test
    fun `하위 항목 순서는 요청 배열 기준으로 1부터 서버가 부여한다`() {
        val persisted = curation(CURATION_ID, CurationStatus.DRAFT)
        `when`(curationRepository.findByWeekStartDate(WEEK_START)).thenReturn(persisted)
        val chapters = listOf(chapter("셋째"), chapter("첫째"), chapter("둘째"))
        val recommendations = listOf(recommendation("둘째"), recommendation("첫째"))

        service.upsert(command(chapters = chapters, recommendations = recommendations))

        val chapterCaptor = chapterIterableCaptor()
        val recommendationCaptor = recommendationIterableCaptor()
        verify(curationChapterRepository).saveAll(captureChapters(chapterCaptor))
        verify(curationRecommendationRepository).saveAll(captureRecommendations(recommendationCaptor))
        val savedChapters = chapterCaptor.value.toList()
        val savedRecommendations = recommendationCaptor.value.toList()
        assertThat(savedChapters.map { it.sortOrder }).containsExactly(1, 2, 3)
        assertThat(savedChapters.map { it.heading }).containsExactly("셋째", "첫째", "둘째")
        assertThat(savedRecommendations.map { it.sortOrder }).containsExactly(1, 2)
        assertThat(savedRecommendations.map { it.title }).containsExactly("둘째", "첫째")
    }

    @Test
    fun `DRAFT에서 PUBLISHED로 바뀌면 발행 시각을 채운다`() {
        val persisted = curation(CURATION_ID, CurationStatus.DRAFT)
        `when`(curationRepository.findByWeekStartDate(WEEK_START)).thenReturn(persisted)

        service.upsert(command(status = CurationStatus.PUBLISHED))

        assertThat(persisted.publishedAt).isNotNull()
    }

    @Test
    fun `이미 발행된 큐레이션은 기존 발행 시각을 유지한다`() {
        val publishedAt = Instant.parse("2026-08-01T00:00:00Z")
        val persisted = curation(CURATION_ID, CurationStatus.PUBLISHED, publishedAt = publishedAt)
        `when`(curationRepository.findByWeekStartDate(WEEK_START)).thenReturn(persisted)

        service.upsert(command(status = CurationStatus.PUBLISHED))

        assertThat(persisted.publishedAt).isEqualTo(publishedAt)
    }

    @Test
    fun `PUBLISHED에서 DRAFT로 내리면 발행 시각을 비운다`() {
        val persisted = curation(
            CURATION_ID,
            CurationStatus.PUBLISHED,
            publishedAt = Instant.parse("2026-08-01T00:00:00Z"),
        )
        `when`(curationRepository.findByWeekStartDate(WEEK_START)).thenReturn(persisted)

        service.upsert(command(status = CurationStatus.DRAFT))

        assertThat(persisted.publishedAt).isNull()
    }

    @Test
    fun `삭제는 챕터와 추천을 먼저 지운 뒤 큐레이션을 지운다`() {
        val persisted = curation(CURATION_ID, CurationStatus.DRAFT)
        `when`(curationRepository.findById(CURATION_ID)).thenReturn(Optional.of(persisted))

        service.delete(CURATION_ID)

        val order = inOrder(curationChapterRepository, curationRecommendationRepository, curationRepository)
        order.verify(curationChapterRepository).deleteByCurationId(CURATION_ID)
        order.verify(curationRecommendationRepository).deleteByCurationId(CURATION_ID)
        order.verify(curationRepository).delete(persisted)
        verify(curationRepository, never()).deleteById(CURATION_ID)
    }

    private fun command(
        title: String = "제목",
        status: CurationStatus = CurationStatus.DRAFT,
        chapters: List<UpsertCurationChapterCommand> = listOf(chapter("챕터")),
        recommendations: List<UpsertCurationRecommendationCommand> = listOf(recommendation("추천")),
    ): UpsertCurationCommand = UpsertCurationCommand(
        weekStartDate = WEEK_START,
        weekEndDate = WEEK_START.plusDays(6),
        weekLabel = "8월 1주차 · 8/1~8/7",
        heroImageUrl = "https://img/hero.jpg",
        title = title,
        subtitle = "부제",
        intro = "도입글",
        nextTeaserOverline = "다음 주 예고",
        nextTeaserBody = "예고 본문",
        status = status,
        chapters = chapters,
        recommendations = recommendations,
    )

    private fun chapter(heading: String): UpsertCurationChapterCommand = UpsertCurationChapterCommand(
        layout = CurationLayout.MAIN,
        heading = heading,
        spotId = 10L,
        placeName = "장소",
        latitude = 37.0,
        longitude = 127.0,
        photoUrl = null,
        pullQuote = null,
        leadText = null,
        body = "본문",
        factNote = "무료",
    )

    private fun recommendation(title: String): UpsertCurationRecommendationCommand =
        UpsertCurationRecommendationCommand(
            title = title,
            spotId = 20L,
            placeName = "추천 장소",
            latitude = 37.0,
            longitude = 127.0,
            photoUrl = null,
            body = "추천 본문",
        )

    private fun curation(
        id: Long,
        status: CurationStatus,
        title: String = "제목",
        publishedAt: Instant? = null,
    ): Curation {
        val curation = Curation(
            weekStartDate = WEEK_START,
            weekEndDate = WEEK_START.plusDays(6),
            weekLabel = "8월 1주차 · 8/1~8/7",
            heroImageUrl = "https://img/hero.jpg",
            title = title,
            subtitle = "부제",
            intro = "도입글",
            nextTeaserOverline = "다음 주 예고",
            nextTeaserBody = "예고 본문",
            status = status,
            publishedAt = publishedAt,
        )
        ReflectionTestUtils.setField(curation, "id", id)
        return curation
    }

    @Suppress("UNCHECKED_CAST")
    private fun chapterIterableCaptor(): ArgumentCaptor<Iterable<CurationChapter>> =
        ArgumentCaptor.forClass(Iterable::class.java) as ArgumentCaptor<Iterable<CurationChapter>>

    @Suppress("UNCHECKED_CAST")
    private fun recommendationIterableCaptor(): ArgumentCaptor<Iterable<CurationRecommendation>> =
        ArgumentCaptor.forClass(Iterable::class.java) as ArgumentCaptor<Iterable<CurationRecommendation>>

    private fun captureChapters(captor: ArgumentCaptor<Iterable<CurationChapter>>): Iterable<CurationChapter> =
        captor.capture() ?: emptyList()

    private fun captureRecommendations(
        captor: ArgumentCaptor<Iterable<CurationRecommendation>>,
    ): Iterable<CurationRecommendation> = captor.capture() ?: emptyList()

    private fun anyCuration(): Curation =
        any(Curation::class.java) ?: curation(Long.MIN_VALUE, CurationStatus.DRAFT)

    companion object {
        private const val CURATION_ID = 101L
        private val WEEK_START = LocalDate.of(2026, 8, 1)
    }
}
