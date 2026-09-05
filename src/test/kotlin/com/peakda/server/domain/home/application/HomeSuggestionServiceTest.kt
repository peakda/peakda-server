package com.peakda.server.domain.home.application

import com.peakda.server.domain.attraction.entity.Attraction
import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.seasonal.entity.Estimator
import com.peakda.server.domain.seasonal.entity.SeasonalBloomEstimate
import com.peakda.server.domain.seasonal.repository.SeasonalBloomEstimateRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate
import java.util.Optional

class HomeSuggestionServiceTest {

    private val attractionRepository = mock(AttractionRepository::class.java)
    private val seasonalBloomEstimateRepository = mock(SeasonalBloomEstimateRepository::class.java)

    private val service = HomeSuggestionService(attractionRepository, seasonalBloomEstimateRepository)

    private val baseDate = LocalDate.of(2026, 3, 30)

    @Test
    fun `산출된 baseDate 가 없으면 available=false 이다`() {
        `when`(seasonalBloomEstimateRepository.findLatestBaseDate()).thenReturn(null)

        val response = service.suggestion()

        assertThat(response.available).isFalse()
        assertThat(response.message).isNull()
        assertThat(response.baseDate).isNull()
    }

    @Test
    fun `절정 명소가 없으면 available=false 이다`() {
        `when`(seasonalBloomEstimateRepository.findLatestBaseDate()).thenReturn(baseDate)
        `when`(seasonalBloomEstimateRepository.findByBaseDateAndStatus(baseDate, BloomStatus.PEAK)).thenReturn(emptyList())

        val response = service.suggestion()

        assertThat(response.available).isFalse()
        assertThat(response.baseDate).isEqualTo(baseDate)
    }

    @Test
    fun `신뢰도가 가장 높은 절정 명소로 카피를 만든다`() {
        `when`(seasonalBloomEstimateRepository.findLatestBaseDate()).thenReturn(baseDate)
        `when`(seasonalBloomEstimateRepository.findByBaseDateAndStatus(baseDate, BloomStatus.PEAK)).thenReturn(
            listOf(
                estimate(ATTRACTION_ID_LOW, BloomCategory.AZALEA_KR, confidence = 0.7),
                estimate(ATTRACTION_ID_HIGH, BloomCategory.CHERRY, confidence = 0.95),
            ),
        )
        `when`(attractionRepository.findById(ATTRACTION_ID_HIGH)).thenReturn(Optional.of(attraction(ATTRACTION_ID_HIGH, "남산")))

        val response = service.suggestion()

        assertThat(response.available).isTrue()
        assertThat(response.category).isEqualTo(BloomCategory.CHERRY)
        assertThat(response.attractionId).isEqualTo(ATTRACTION_ID_HIGH)
        assertThat(response.attractionTitle).isEqualTo("남산")
        assertThat(response.message).isEqualTo("요즘 절정인 벚꽃, 남산에서 만나보세요")
    }

    @Test
    fun `최고 신뢰도 명소의 Attraction 행이 없으면 available=false 이다`() {
        `when`(seasonalBloomEstimateRepository.findLatestBaseDate()).thenReturn(baseDate)
        `when`(seasonalBloomEstimateRepository.findByBaseDateAndStatus(baseDate, BloomStatus.PEAK))
            .thenReturn(listOf(estimate(ATTRACTION_ID_HIGH, BloomCategory.CHERRY, confidence = 0.95)))
        `when`(attractionRepository.findById(ATTRACTION_ID_HIGH)).thenReturn(Optional.empty())

        val response = service.suggestion()

        assertThat(response.available).isFalse()
    }

    private fun estimate(attractionId: Long, category: BloomCategory, confidence: Double) = SeasonalBloomEstimate(
        attractionId = attractionId,
        bloomCategory = category,
        baseDate = baseDate,
        status = BloomStatus.PEAK,
        confidence = confidence,
        chosenEstimator = Estimator.CALENDAR,
    )

    private fun attraction(id: Long, title: String): Attraction {
        val attraction = Attraction(
            tourApiContentId = "c-$id",
            title = title,
            latitude = 37.55,
            longitude = 126.98,
        )
        ReflectionTestUtils.setField(attraction, "id", id)
        return attraction
    }

    companion object {
        private const val ATTRACTION_ID_HIGH = 501L
        private const val ATTRACTION_ID_LOW = 502L
    }
}
