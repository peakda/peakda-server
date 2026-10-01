package com.peakda.server.domain.visittiming.application

import com.peakda.server.domain.congestion.application.DailyCongestion
import com.peakda.server.domain.festival.application.FestivalPhase
import com.peakda.server.domain.festival.application.NearbyFestival
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.weather.application.DailyWeather
import com.peakda.server.domain.weather.application.WeatherSky
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VisitTimingRecommenderTest {
    private val properties = VisitTimingProperties()
    private val today = LocalDate.of(2026, 4, 1)
    private val dates = (0L until 7L).map { today.plusDays(it) }

    @Test
    fun `비 오는 날을 빼고 집중률이 가장 낮은 날을 고른다`() {
        val congestion = congestionOf(70.0, 30.0, 20.0, 60.0)
        val weather = mapOf(
            today.plusDays(1) to weather(today.plusDays(1), WeatherSky.CLEAR, 10),
            today.plusDays(2) to weather(today.plusDays(2), WeatherSky.RAIN, 80),
        )

        val result = recommend(congestion, weather)

        assertThat(result?.date).isEqualTo(today.plusDays(1))
        assertThat(result?.reasons).containsExactly(VisitReason.LOW_CONGESTION, VisitReason.CLEAR_WEATHER)
    }

    @Test
    fun `가장 낮은 집중률이면 가장 한산한 날 이유를 붙인다`() {
        val result = recommend(congestionOf(70.0, 40.0, 50.0), emptyMap())

        assertThat(result?.date).isEqualTo(today.plusDays(1))
        assertThat(result?.reasons).containsExactly(VisitReason.LEAST_CROWDED)
    }

    @Test
    fun `개화 정보가 있으면 꽃을 볼 수 있는 날만 후보로 두고 절정이면 이유를 붙인다`() {
        val congestion = congestionOf(10.0, 50.0, 60.0)
        val bloom = mapOf(
            today to BloomStatus.PREPARING,
            today.plusDays(1) to BloomStatus.STARTED,
            today.plusDays(2) to BloomStatus.PEAK,
        )

        val result = recommend(congestion, emptyMap(), bloom)

        assertThat(result?.date).isEqualTo(today.plusDays(1))
        assertThat(recommend(congestionOf(10.0, 70.0, 60.0), emptyMap(), bloom)?.reasons)
            .contains(VisitReason.BLOOM_PEAK)
    }

    @Test
    fun `꽃을 볼 수 있는 날이 없으면 추천하지 않는다`() {
        val bloom = dates.associateWith { BloomStatus.ENDED }

        assertThat(recommend(congestionOf(10.0, 20.0), emptyMap(), bloom)).isNull()
    }

    @Test
    fun `혼잡도도 날씨도 모르면 추천하지 않는다`() {
        assertThat(recommend(emptyMap(), emptyMap())).isNull()
    }

    @Test
    fun `그날 열리는 주변 축제가 있으면 이유에 넣는다`() {
        val festival = NearbyFestival(1, "진해군항제", today, today.plusDays(3), FestivalPhase.ONGOING, 300.0)

        val result = VisitTimingRecommender.recommend(dates, congestionOf(10.0), emptyMap(), null, listOf(festival), properties)

        assertThat(result?.reasons).contains(VisitReason.FESTIVAL_ONGOING)
    }

    private fun recommend(
        congestion: Map<LocalDate, DailyCongestion>,
        weather: Map<LocalDate, DailyWeather>,
        bloom: Map<LocalDate, BloomStatus>? = null,
    ) = VisitTimingRecommender.recommend(dates, congestion, weather, bloom, emptyList(), properties)

    private fun congestionOf(vararg rates: Double): Map<LocalDate, DailyCongestion> =
        rates.withIndex().associate { (index, rate) ->
            today.plusDays(index.toLong()) to DailyCongestion(today.plusDays(index.toLong()), rate)
        }

    private fun weather(date: LocalDate, sky: WeatherSky, probability: Int) =
        DailyWeather(date, sky, probability, 10.0, 20.0)
}
