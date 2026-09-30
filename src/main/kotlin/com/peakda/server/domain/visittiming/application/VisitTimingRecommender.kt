package com.peakda.server.domain.visittiming.application

import com.peakda.server.domain.congestion.application.DailyCongestion
import com.peakda.server.domain.festival.application.NearbyFestival
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.weather.application.DailyWeather
import com.peakda.server.domain.weather.application.WeatherSky
import java.time.LocalDate

/**
 * 향후 며칠 가운데 방문하기 좋은 날 하나를 고른다.
 *
 * 1. 개화 정보가 있으면 꽃을 볼 수 있는 날(시작·절정)만 후보로 둔다. 하루도 없으면 추천하지 않는다.
 * 2. 비 올 가능성이 높은 날은 뺀다.
 * 3. 남은 후보 중 집중률이 가장 낮은 날, 같으면 강수확률이 낮은 날, 그래도 같으면 이른 날.
 *
 * 혼잡도도 날씨도 모르는 날은 고를 근거가 없어 후보에서 뺀다.
 */
object VisitTimingRecommender {
    fun recommend(
        dates: List<LocalDate>,
        congestion: Map<LocalDate, DailyCongestion>,
        weather: Map<LocalDate, DailyWeather>,
        bloomStatusByDate: Map<LocalDate, BloomStatus>?,
        festivals: List<NearbyFestival>,
        properties: VisitTimingProperties,
    ): VisitRecommendation? {
        val candidates = dates
            .filter { it in congestion || it in weather }
            .filter { bloomStatusByDate == null || bloomStatusByDate[it] in VISIBLE_BLOOM }
            .filterNot { isRainy(weather[it], properties) }
        val chosen = candidates.minWithOrNull(
            compareBy<LocalDate> { congestion[it]?.rate ?: UNKNOWN_RATE }
                .thenBy { weather[it]?.precipitationProbability ?: 0 }
                .thenBy { it },
        ) ?: return null

        val reasons = reasons(chosen, congestion, weather[chosen], bloomStatusByDate?.get(chosen), festivals, properties)
        return VisitRecommendation(chosen, reasons)
    }

    private fun reasons(
        date: LocalDate,
        congestion: Map<LocalDate, DailyCongestion>,
        weather: DailyWeather?,
        bloomStatus: BloomStatus?,
        festivals: List<NearbyFestival>,
        properties: VisitTimingProperties,
    ): List<VisitReason> = buildList {
        if (bloomStatus == BloomStatus.PEAK) add(VisitReason.BLOOM_PEAK)
        congestion[date]?.let { chosen ->
            if (congestion.size > 1 && congestion.values.all { chosen.rate <= it.rate }) add(VisitReason.LEAST_CROWDED)
            if (CongestionLevel.of(chosen.rate, properties.congestion) == CongestionLevel.QUIET) {
                add(VisitReason.LOW_CONGESTION)
            }
        }
        if (weather != null && isClear(weather, properties)) add(VisitReason.CLEAR_WEATHER)
        if (festivals.any { date in it.startsOn..it.endsOn }) add(VisitReason.FESTIVAL_ONGOING)
    }

    private fun isRainy(weather: DailyWeather?, properties: VisitTimingProperties): Boolean {
        weather ?: return false
        val probability = weather.precipitationProbability
        if (probability != null) return probability >= properties.rainyProbabilityThreshold
        return weather.sky?.precipitating == true
    }

    private fun isClear(weather: DailyWeather, properties: VisitTimingProperties): Boolean {
        val sky = weather.sky ?: return false
        val probability = weather.precipitationProbability ?: return false
        return sky <= WeatherSky.PARTLY_CLOUDY && probability < properties.clearProbabilityThreshold
    }

    /** 혼잡도를 모르는 날은 중간값으로 보고 비교한다. */
    private const val UNKNOWN_RATE = 50.0

    private val VISIBLE_BLOOM = setOf(BloomStatus.STARTED, BloomStatus.PEAK)
}
