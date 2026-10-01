package com.peakda.server.domain.visittiming.application

import com.peakda.server.domain.congestion.application.CongestionForecastService
import com.peakda.server.domain.congestion.application.DailyCongestion
import com.peakda.server.domain.festival.application.NearbyFestival
import com.peakda.server.domain.festival.application.NearbyFestivalService
import com.peakda.server.domain.seasonal.application.BloomStatusWindowResolver
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.visittiming.presentation.response.VisitTimingResponse
import com.peakda.server.domain.weather.application.AttractionWeatherForecast
import com.peakda.server.domain.weather.application.AttractionWeatherForecastService
import com.peakda.server.domain.weather.application.DailyWeather
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/**
 * "지금 가기 좋은가" — 개화 상태에 향후 혼잡도·날씨·주변 축제를 붙이고 추천 방문일을 고른다.
 *
 * 각 도메인은 자기 조회 서비스로만 읽고, 조합과 판단은 여기서 한다(도메인 간 조인 없음).
 *
 * 명소 상세의 부가 정보라서 한 출처가 실패해도 상세 전체를 실패시키지 않는다. 출처별로 예외를 잡아 그 항목만
 * 비운다. 하위 조회 서비스는 일부러 `@Transactional` 을 달지 않는다 — 트랜잭션 프록시를 지나는 예외는
 * 바깥(명소 상세) 트랜잭션을 rollback-only 로 만들어, 여기서 잡아도 커밋 시점에 실패하기 때문이다.
 */
@Service
class VisitTimingService(
    private val congestionForecastService: CongestionForecastService,
    private val attractionWeatherForecastService: AttractionWeatherForecastService,
    private val nearbyFestivalService: NearbyFestivalService,
    private val bloomStatusWindowResolver: BloomStatusWindowResolver,
    private val properties: VisitTimingProperties,
    private val clock: Clock = Clock.system(KST),
) {
    /** 보여줄 데이터가 하나도 없으면 null. */
    fun resolve(attractionId: Long, latitude: Double, longitude: Double, bloom: VisitTimingBloom?): VisitTimingResponse? {
        val today = LocalDate.now(clock)
        val dates = (0 until properties.forecastDays.coerceAtLeast(1)).map { today.plusDays(it) }
        val until = dates.last()

        val congestion = guarded("congestion", attractionId) {
            congestionForecastService.findDailyForecast(attractionId, today, until)
        }.orEmpty().associateBy(DailyCongestion::date)
        val weather = guarded("weather", attractionId) {
            attractionWeatherForecastService.findForecast(attractionId, today, until)
        }
        val weatherByDate = weather?.daily.orEmpty().associateBy(DailyWeather::date)
        val festivals = guarded("festival", attractionId) {
            nearbyFestivalService.findNearby(
                latitude = latitude,
                longitude = longitude,
                today = today,
                radiusMeters = properties.festivalRadiusMeters,
                lookaheadDays = properties.festivalLookaheadDays,
                limit = properties.maxFestivals,
            )
        }.orEmpty()
        if (congestion.isEmpty() && weatherByDate.isEmpty() && festivals.isEmpty()) return null

        val recommendation = VisitTimingRecommender.recommend(
            dates = dates,
            congestion = congestion,
            weather = weatherByDate,
            bloomStatusByDate = bloom?.let { bloomStatusByDate(it, dates) },
            festivals = festivals,
            properties = properties,
        )
        return VisitTimingResponse(
            congestion = congestion.takeIf { it.isNotEmpty() }?.let { toCongestion(it, today) },
            weather = weather?.takeIf { it.daily.isNotEmpty() || it.rainWindows.isNotEmpty() }?.let(::toWeather),
            nearbyFestivals = festivals.map(::toFestival),
            recommendation = recommendation?.let { VisitTimingResponse.Recommendation(it.date, it.reasons) },
        )
    }

    private fun <T> guarded(source: String, attractionId: Long, block: () -> T): T? =
        runCatching(block)
            .onFailure { log.warn("[visitTiming] source={} attractionId={} failed: {}", source, attractionId, it.message, it) }
            .getOrNull()

    /** 절정 구간을 알면 날짜별로 다시 판정하고, 모르면 현재 상태가 기간 내내 이어진다고 본다. */
    private fun bloomStatusByDate(bloom: VisitTimingBloom, dates: List<LocalDate>): Map<LocalDate, BloomStatus> =
        dates.associateWith { date ->
            bloomStatusWindowResolver.statusOn(date, bloom.peakStartDate, bloom.peakEndDate) ?: bloom.status
        }

    private fun toCongestion(byDate: Map<LocalDate, DailyCongestion>, today: LocalDate): VisitTimingResponse.CongestionForecast {
        val days = byDate.values.sortedBy(DailyCongestion::date).map { day ->
            VisitTimingResponse.DailyCongestion(
                date = day.date,
                rate = (day.rate * 10).roundToInt() / 10.0,
                level = CongestionLevel.of(day.rate, properties.congestion),
            )
        }
        return VisitTimingResponse.CongestionForecast(
            today = days.firstOrNull { it.date == today },
            days = days,
            quietestDate = days.minWithOrNull(compareBy({ it.rate }, { it.date }))?.date,
        )
    }

    private fun toWeather(weather: AttractionWeatherForecast) = VisitTimingResponse.WeatherForecast(
        days = weather.daily.map {
            VisitTimingResponse.DailyWeather(it.date, it.sky, it.precipitationProbability, it.minTemperature, it.maxTemperature)
        },
        rainWindows = weather.rainWindows.map { VisitTimingResponse.RainWindow(it.start, it.end, it.sky) },
    )

    private fun toFestival(festival: NearbyFestival) = VisitTimingResponse.NearbyFestivalItem(
        festivalId = festival.festivalId,
        name = festival.name,
        phase = festival.phase,
        startsOn = festival.startsOn,
        endsOn = festival.endsOn,
        distanceMeters = festival.distanceMeters.roundToInt(),
    )

    companion object {
        private val KST: ZoneId = ZoneId.of("Asia/Seoul")
        private val log = LoggerFactory.getLogger(VisitTimingService::class.java)
    }
}
