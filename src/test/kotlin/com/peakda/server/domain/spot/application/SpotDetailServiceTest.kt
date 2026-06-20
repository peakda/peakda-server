package com.peakda.server.domain.spot.application

import com.peakda.server.domain.attraction.entity.Attraction
import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.seasonal.entity.Estimator
import com.peakda.server.domain.seasonal.entity.SeasonalBloomEstimate
import com.peakda.server.domain.seasonal.repository.SeasonalBloomEstimateRepository
import com.peakda.server.domain.spot.entity.Spot
import com.peakda.server.domain.spot.entity.SpotFavorite
import com.peakda.server.domain.spot.entity.SpotRecord
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.entity.SpotType
import com.peakda.server.domain.spot.exception.SpotNotFoundException
import com.peakda.server.domain.spot.presentation.response.SpotRecordResponse
import com.peakda.server.domain.spot.presentation.response.SpotRecordSummaryResponse
import com.peakda.server.domain.spot.repository.SpotFavoriteRepository
import com.peakda.server.domain.spot.repository.SpotRecordRepository
import com.peakda.server.domain.spot.repository.SpotRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.test.util.ReflectionTestUtils
import java.time.Instant
import java.time.LocalDate
import java.util.Optional

class SpotDetailServiceTest {

    private val spotRepository = mock(SpotRepository::class.java)
    private val attractionRepository = mock(AttractionRepository::class.java)
    private val seasonalBloomEstimateRepository = mock(SeasonalBloomEstimateRepository::class.java)
    private val spotRecordRepository = mock(SpotRecordRepository::class.java)
    private val spotFavoriteRepository = mock(SpotFavoriteRepository::class.java)
    private val assembler = mock(SpotRecordResponseAssembler::class.java)

    private val service = SpotDetailService(
        spotRepository,
        attractionRepository,
        seasonalBloomEstimateRepository,
        spotRecordRepository,
        spotFavoriteRepository,
        assembler,
    )

    private val baseDate = LocalDate.of(2026, 3, 30)

    @Test
    fun `명소 스팟은 명소 이미지와 채택된 개화 배너, 찜 상태를 함께 반환한다`() {
        val spot = attractionSpot(primaryImageUrl = "https://img/primary.jpg")
        stubRecords(count = 12, preview = listOf(summary(coverUrl = "https://rec/cover.jpg")))
        stubFavorite(SpotFavorite(userId = USER_ID, spotId = SPOT_ID, notifyEnabled = true))
        // 상태 우선순위(PEAK > ENDED)가 신뢰도보다 먼저 적용되어야 한다.
        stubEstimates(
            estimate(BloomStatus.ENDED, confidence = 0.9),
            estimate(BloomStatus.PEAK, confidence = 0.7),
        )

        val response = service.getDetail(SPOT_ID, USER_ID)

        assertThat(response.id).isEqualTo(SPOT_ID)
        assertThat(response.representativeImageUrl).isEqualTo("https://img/primary.jpg")
        assertThat(response.recordCount).isEqualTo(12)
        assertThat(response.recordPreview).hasSize(1)
        assertThat(response.bloom).isNotNull
        assertThat(response.bloom!!.status).isEqualTo(BloomStatus.PEAK)
        assertThat(response.bloom!!.category).isEqualTo(BloomCategory.CHERRY)
        assertThat(response.bloom!!.confidence).isEqualTo(0.7)
        assertThat(response.bloom!!.baseDate).isEqualTo(baseDate)
        assertThat(response.favorite.favorited).isTrue()
        assertThat(response.favorite.notifyEnabled).isTrue()
    }

    @Test
    fun `명소 이미지가 없으면 최근 기록의 대표 사진으로 대체한다`() {
        val spot = attractionSpot(primaryImageUrl = null, thumbnailImageUrl = null)
        stubRecords(count = 1, preview = listOf(summary(coverUrl = "https://rec/cover.jpg")))
        stubFavorite(null)
        stubEstimates()

        val response = service.getDetail(SPOT_ID, USER_ID)

        assertThat(response.representativeImageUrl).isEqualTo("https://rec/cover.jpg")
    }

    @Test
    fun `개화 추정이 없으면 만개 배너는 null 이다`() {
        attractionSpot(primaryImageUrl = "https://img/primary.jpg")
        stubRecords(count = 0, preview = emptyList())
        stubFavorite(null)
        stubEstimates()

        val response = service.getDetail(SPOT_ID, USER_ID)

        assertThat(response.bloom).isNull()
    }

    @Test
    fun `찜하지 않았으면 favorited 와 notifyEnabled 는 모두 false 다`() {
        attractionSpot(primaryImageUrl = "https://img/primary.jpg")
        stubRecords(count = 0, preview = emptyList())
        stubFavorite(null)
        stubEstimates()

        val response = service.getDetail(SPOT_ID, USER_ID)

        assertThat(response.favorite.favorited).isFalse()
        assertThat(response.favorite.notifyEnabled).isFalse()
    }

    @Test
    fun `존재하지 않는 스팟이면 SpotNotFoundException 을 던진다`() {
        `when`(spotRepository.findById(SPOT_ID)).thenReturn(Optional.empty())

        assertThatThrownBy { service.getDetail(SPOT_ID, USER_ID) }
            .isInstanceOf(SpotNotFoundException::class.java)
    }

    private fun attractionSpot(
        primaryImageUrl: String?,
        thumbnailImageUrl: String? = null,
    ): Spot {
        val spot = Spot(
            type = SpotType.ATTRACTION,
            attractionId = ATTRACTION_ID,
            name = "진해 여좌천",
            address = "경상남도 창원시 진해구 여좌동",
            latitude = 35.1533,
            longitude = 128.6712,
        )
        ReflectionTestUtils.setField(spot, "id", SPOT_ID)
        `when`(spotRepository.findById(SPOT_ID)).thenReturn(Optional.of(spot))

        val attraction = Attraction(
            tourApiContentId = "c-1",
            title = "진해 여좌천",
            primaryImageUrl = primaryImageUrl,
            thumbnailImageUrl = thumbnailImageUrl,
        )
        ReflectionTestUtils.setField(attraction, "id", ATTRACTION_ID)
        `when`(attractionRepository.findById(ATTRACTION_ID)).thenReturn(Optional.of(attraction))
        return spot
    }

    private fun stubRecords(count: Long, preview: List<SpotRecordSummaryResponse>) {
        `when`(spotRecordRepository.countBySpotIdAndStatus(SPOT_ID, SpotRecordStatus.PUBLISHED)).thenReturn(count)
        `when`(
            spotRecordRepository.findBySpotIdAndStatusOrderByCreatedAtDesc(
                SPOT_ID,
                SpotRecordStatus.PUBLISHED,
                PageRequest.of(0, PREVIEW_SIZE),
            ),
        ).thenReturn(PageImpl(emptyList<SpotRecord>()))
        `when`(assembler.assembleSummaries(anyList())).thenReturn(preview)
    }

    private fun stubFavorite(favorite: SpotFavorite?) {
        `when`(spotFavoriteRepository.findByUserIdAndSpotId(USER_ID, SPOT_ID)).thenReturn(favorite)
    }

    private fun stubEstimates(vararg estimates: SeasonalBloomEstimate) {
        `when`(seasonalBloomEstimateRepository.findLatestBaseDate()).thenReturn(baseDate)
        `when`(seasonalBloomEstimateRepository.findByAttractionIdAndBaseDate(ATTRACTION_ID, baseDate))
            .thenReturn(estimates.toList())
    }

    private fun estimate(status: BloomStatus, confidence: Double) = SeasonalBloomEstimate(
        attractionId = ATTRACTION_ID,
        bloomCategory = BloomCategory.CHERRY,
        baseDate = baseDate,
        status = status,
        confidence = confidence,
        chosenEstimator = Estimator.CALENDAR,
        peakStartDate = LocalDate.of(2026, 3, 28),
        peakEndDate = LocalDate.of(2026, 4, 5),
        peakDurationDays = 9,
    )

    private fun summary(coverUrl: String) = SpotRecordSummaryResponse(
        id = 1L,
        spotId = SPOT_ID,
        spotName = "진해 여좌천",
        user = SpotRecordResponse.UserSummary(id = USER_ID, nickname = "tester", profileImageUrl = null),
        visitedDate = LocalDate.of(2026, 3, 30),
        bloomStage = null,
        memo = null,
        plants = emptyList(),
        coverPhoto = SpotRecordResponse.PhotoEntry(objectKey = "k", url = coverUrl, sortOrder = 0),
        status = SpotRecordStatus.PUBLISHED,
        publishedAt = Instant.now(),
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    companion object {
        private const val SPOT_ID = 100L
        private const val ATTRACTION_ID = 501L
        private const val USER_ID = 7L
        private const val PREVIEW_SIZE = 3
    }
}
