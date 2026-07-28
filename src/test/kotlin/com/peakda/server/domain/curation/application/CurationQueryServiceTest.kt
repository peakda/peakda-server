package com.peakda.server.domain.curation.application

import com.peakda.server.common.storage.ObjectKeyUrlResolver
import com.peakda.server.domain.curation.entity.Curation
import com.peakda.server.domain.curation.entity.CurationChapter
import com.peakda.server.domain.curation.entity.CurationLayout
import com.peakda.server.domain.curation.entity.CurationRecommendation
import com.peakda.server.domain.curation.entity.CurationStatus
import com.peakda.server.domain.curation.exception.CurationNotFoundException
import com.peakda.server.domain.curation.repository.CurationChapterRepository
import com.peakda.server.domain.curation.repository.CurationRecommendationRepository
import com.peakda.server.domain.curation.repository.CurationRepository
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.spot.application.SpotPreviewService
import com.peakda.server.domain.spot.entity.SpotType
import com.peakda.server.domain.spot.presentation.response.SpotPreviewResponse
import com.peakda.server.domain.spot.presentation.response.SpotPreviewResponse.BloomBadge
import com.peakda.server.domain.spot.presentation.response.SpotPreviewResponse.SpotPreviewItem
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate

class CurationQueryServiceTest {

    private val curationRepository = mock(CurationRepository::class.java)
    private val curationChapterRepository = mock(CurationChapterRepository::class.java)
    private val curationRecommendationRepository = mock(CurationRecommendationRepository::class.java)
    private val spotPreviewService = mock(SpotPreviewService::class.java)
    private val objectKeyUrlResolver = mock(ObjectKeyUrlResolver::class.java)
    private val service = CurationQueryService(
        curationRepository,
        curationChapterRepository,
        curationRecommendationRepository,
        spotPreviewService,
        objectKeyUrlResolver,
    )

    @Test
    fun `카드는 발행 상태만 최신 주차순 파인더로 조회한다`() {
        val pageable = PageRequest.of(0, 5)
        val curation = curation(1L, CurationStatus.PUBLISHED)
        `when`(
            curationRepository.findByStatusOrderByWeekStartDateDesc(CurationStatus.PUBLISHED, pageable),
        ).thenReturn(PageImpl(listOf(curation), pageable, 1))

        val response = service.cards(pageable)

        assertThat(response.content.map { it.id }).containsExactly(1L)
        verify(curationRepository).findByStatusOrderByWeekStartDateDesc(CurationStatus.PUBLISHED, pageable)
    }

    @Test
    fun `DRAFT 큐레이션 상세은 찾을 수 없음 예외다`() {
        `when`(curationRepository.findByIdAndStatus(1L, CurationStatus.PUBLISHED)).thenReturn(null)

        assertThatThrownBy { service.detail(1L, lat = null, lng = null) }
            .isInstanceOf(CurationNotFoundException::class.java)
    }

    @Test
    fun `미존재 큐레이션 상세은 찾을 수 없음 예외다`() {
        `when`(curationRepository.findByIdAndStatus(999L, CurationStatus.PUBLISHED)).thenReturn(null)

        assertThatThrownBy { service.detail(999L, lat = null, lng = null) }
            .isInstanceOf(CurationNotFoundException::class.java)
    }

    @Test
    fun `챕터와 추천 스팟을 한 번에 프리뷰하고 뱃지와 거리를 정확히 매핑한다`() {
        val chapters = listOf(
            chapter(id = 1L, sortOrder = 1, spotId = 10L),
            chapter(id = 2L, sortOrder = 2, spotId = 20L),
        )
        val recommendations = listOf(
            recommendation(id = 3L, sortOrder = 1, spotId = 20L),
        )
        stubDetail(chapters, recommendations)
        val cherryBadge = BloomBadge(BloomCategory.CHERRY, "벚꽃", BloomStatus.PEAK)
        val plumBadge = BloomBadge(BloomCategory.PLUM, "매화", BloomStatus.STARTED)
        `when`(spotPreviewService.preview(listOf(10L, 20L), category = null, lat = LAT, lng = LNG)).thenReturn(
            SpotPreviewResponse(
                listOf(
                    preview(10L, "https://img/10.jpg", cherryBadge, 100.0),
                    preview(20L, "https://img/20.jpg", plumBadge, 200.0),
                ),
            ),
        )

        val response = service.detail(CURATION_ID, LAT, LNG)

        assertThat(response.chapters.map { it.badge }).containsExactly(cherryBadge, plumBadge)
        assertThat(response.chapters.map { it.distanceMeters }).containsExactly(100.0, 200.0)
        assertThat(response.recommendations.single().distanceMeters).isEqualTo(200.0)
        verify(spotPreviewService, times(1)).preview(listOf(10L, 20L), category = null, lat = LAT, lng = LNG)
    }

    @Test
    fun `사진이 없으면 프리뷰 썸네일을 쓰고 스팟 없는 챕터는 뱃지와 거리가 없다`() {
        val chapters = listOf(
            chapter(id = 1L, sortOrder = 1, spotId = 10L, photoUrl = null),
            chapter(id = 2L, sortOrder = 2, spotId = null, photoUrl = null),
        )
        stubDetail(chapters, emptyList())
        val badge = BloomBadge(BloomCategory.CHERRY, "벚꽃", BloomStatus.PEAK)
        `when`(spotPreviewService.preview(listOf(10L), category = null, lat = LAT, lng = LNG)).thenReturn(
            SpotPreviewResponse(listOf(preview(10L, "https://img/preview.jpg", badge, 123.0))),
        )

        val response = service.detail(CURATION_ID, LAT, LNG)

        assertThat(response.chapters.map { it.sortOrder }).containsExactly(1, 2)
        assertThat(response.chapters.first().photoUrl).isEqualTo("https://img/preview.jpg")
        assertThat(response.chapters.last().badge).isNull()
        assertThat(response.chapters.last().distanceMeters).isNull()
    }

    @Test
    fun `챕터와 추천은 정렬 파인더가 반환한 순서를 보존한다`() {
        val chapters = listOf(chapter(1L, 1, null), chapter(2L, 2, null), chapter(3L, 3, null))
        val recommendations = listOf(recommendation(4L, 1, null), recommendation(5L, 2, null))
        stubDetail(chapters, recommendations)
        `when`(spotPreviewService.preview(emptyList(), category = null, lat = null, lng = null))
            .thenReturn(SpotPreviewResponse(emptyList()))

        val response = service.detail(CURATION_ID, lat = null, lng = null)

        assertThat(response.chapters.map { it.sortOrder }).containsExactly(1, 2, 3)
        assertThat(response.recommendations.map { it.sortOrder }).containsExactly(1, 2)
        verify(curationChapterRepository).findByCurationIdOrderBySortOrderAsc(CURATION_ID)
        verify(curationRecommendationRepository).findByCurationIdOrderBySortOrderAsc(CURATION_ID)
    }

    private fun stubDetail(
        chapters: List<CurationChapter>,
        recommendations: List<CurationRecommendation>,
    ) {
        `when`(curationRepository.findByIdAndStatus(CURATION_ID, CurationStatus.PUBLISHED))
            .thenReturn(curation(CURATION_ID, CurationStatus.PUBLISHED))
        `when`(curationChapterRepository.findByCurationIdOrderBySortOrderAsc(CURATION_ID)).thenReturn(chapters)
        `when`(curationRecommendationRepository.findByCurationIdOrderBySortOrderAsc(CURATION_ID))
            .thenReturn(recommendations)
    }

    private fun curation(id: Long, status: CurationStatus): Curation {
        val curation = Curation(
            weekStartDate = LocalDate.of(2026, 8, 1),
            weekEndDate = LocalDate.of(2026, 8, 7),
            weekLabel = "8월 1주차 · 8/1~8/7",
            heroImageUrl = "https://img/hero.jpg",
            title = "이번 주말, 노란색을 보러 가야 해요",
            subtitle = "해바라기가 가장 예쁜 세 곳",
            intro = "도입글",
            nextTeaserOverline = "다음 주 예고",
            nextTeaserBody = "한 주 더 노란색이 이어져요.",
            status = status,
        )
        ReflectionTestUtils.setField(curation, "id", id)
        return curation
    }

    private fun chapter(
        id: Long,
        sortOrder: Int,
        spotId: Long?,
        photoUrl: String? = "https://img/chapter-$id.jpg",
    ): CurationChapter {
        val chapter = CurationChapter(
            curationId = CURATION_ID,
            sortOrder = sortOrder,
            layout = CurationLayout.MAIN,
            heading = "챕터 $sortOrder",
            spotId = spotId,
            placeName = "장소 $sortOrder",
            latitude = 37.0,
            longitude = 127.0,
            photoUrl = photoUrl,
            body = "본문",
        )
        ReflectionTestUtils.setField(chapter, "id", id)
        return chapter
    }

    private fun recommendation(id: Long, sortOrder: Int, spotId: Long?): CurationRecommendation {
        val recommendation = CurationRecommendation(
            curationId = CURATION_ID,
            sortOrder = sortOrder,
            title = "추천 $sortOrder",
            spotId = spotId,
            placeName = "추천 장소 $sortOrder",
            latitude = 37.0,
            longitude = 127.0,
            photoUrl = null,
            body = "추천 본문",
        )
        ReflectionTestUtils.setField(recommendation, "id", id)
        return recommendation
    }

    private fun preview(
        spotId: Long,
        thumbnailUrl: String,
        badge: BloomBadge,
        distanceMeters: Double,
    ): SpotPreviewItem = SpotPreviewItem(
        spotId = spotId,
        type = SpotType.ATTRACTION,
        name = "스팟 $spotId",
        thumbnailUrl = thumbnailUrl,
        badge = badge,
        distanceMeters = distanceMeters,
    )

    companion object {
        private const val CURATION_ID = 101L
        private const val LAT = 37.5
        private const val LNG = 127.0
    }
}
