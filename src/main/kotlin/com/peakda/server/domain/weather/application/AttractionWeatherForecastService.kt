package com.peakda.server.domain.weather.application

import com.peakda.server.domain.weather.entity.WeatherMidForecast
import com.peakda.server.domain.weather.entity.WeatherShortForecast
import com.peakda.server.domain.weather.repository.AttractionForecastAreaRepository
import com.peakda.server.domain.weather.repository.WeatherMidForecastRepository
import com.peakda.server.domain.weather.repository.WeatherShortForecastRepository
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 명소의 일별 날씨와 가까운 강수 구간을 만든다.
 *
 * 예보 구역 매핑([AttractionForecastAreaMappingService])이 있는 명소만 대상이다. 같은 날짜는
 * 예측 시점이 가까운 단기예보를 중기예보보다 우선한다.
 */
@Service
class AttractionWeatherForecastService(
    private val forecastAreaRepository: AttractionForecastAreaRepository,
    private val shortForecastRepository: WeatherShortForecastRepository,
    private val midForecastRepository: WeatherMidForecastRepository,
    private val clock: Clock = Clock.system(KST),
) {
    /** [from]~[to] (양 끝 포함). 예보 구역 매핑이 없으면 null. */
    fun findForecast(attractionId: Long, from: LocalDate, to: LocalDate): AttractionWeatherForecast? {
        val area = forecastAreaRepository.findByAttractionId(attractionId) ?: return null
        val shortRows = shortForecastRepository.findByGridXAndGridYAndForecastCategoryInAndForecastDateBetween(
            gridX = area.gridX,
            gridY = area.gridY,
            forecastCategories = SHORT_CATEGORIES,
            forecastDateStart = from.format(BASIC),
            forecastDateEnd = to.format(BASIC),
        )
        val shortDaily = summarizeShort(shortRows).associateBy(DailyWeather::date)
        val midDaily = area.midRegionCode
            ?.let(midForecastRepository::findFirstByRegionCodeOrderByAnnounceTimeDesc)
            ?.let(::summarizeMid)
            .orEmpty()
            .filter { it.date in from..to }
            .associateBy(DailyWeather::date)

        val daily = (midDaily.keys + shortDaily.keys).sorted().mapNotNull { date ->
            mergeDay(shortDaily[date], midDaily[date])
        }
        val now = LocalDateTime.now(clock)
        val rainWindows = rainWindows(shortRows).filter { it.end.isAfter(now) }
        return AttractionWeatherForecast(daily, rainWindows)
    }

    /**
     * 단기예보의 마지막 날은 밤 시간만 있어 낮 대표값이 비기도 한다. 그런 날까지 단기예보로 통째로 덮으면
     * 멀쩡한 중기예보 값이 사라지므로 항목별로 단기 값이 있으면 쓰고 없으면 중기 값으로 채운다.
     */
    private fun mergeDay(short: DailyWeather?, mid: DailyWeather?): DailyWeather? {
        if (short == null || mid == null) return short ?: mid
        return DailyWeather(
            date = short.date,
            sky = short.sky ?: mid.sky,
            precipitationProbability = short.precipitationProbability ?: mid.precipitationProbability,
            minTemperature = short.minTemperature ?: mid.minTemperature,
            maxTemperature = short.maxTemperature ?: mid.maxTemperature,
        )
    }

    private fun summarizeShort(rows: List<WeatherShortForecast>): List<DailyWeather> =
        rows.groupBy { it.forecastDate }.mapNotNull { (forecastDate, dayRows) ->
            val date = parseDate(forecastDate) ?: return@mapNotNull null
            val daytime = dayRows.filter { hourOf(it) in DAYTIME_HOURS }
            val precipitation = daytime.categoryValues(PTY).mapNotNull(WeatherSky::fromPrecipitationCode)
            val sky = precipitation.maxOrNull()
                ?: daytime.categoryValues(SKY).mapNotNull(WeatherSky::fromShortSkyCode).mostFrequentWorst()
            // TMN·TMX 가 없는 날(예보 범위 끝의 일부 시간만 있는 날)은 낮 시간 기온으로만 대신한다. 밤 기온 한두 개를 최고기온으로 쓰면 틀린다.
            val temperatures = daytime.categoryValues(TMP).mapNotNull(String::toDoubleOrNull)
            DailyWeather(
                date = date,
                sky = sky,
                precipitationProbability = daytime.categoryValues(POP).mapNotNull(String::toIntOrNull).maxOrNull(),
                minTemperature = dayRows.categoryValues(TMN).firstNotNullOfOrNull(String::toDoubleOrNull)
                    ?: temperatures.minOrNull(),
                maxTemperature = dayRows.categoryValues(TMX).firstNotNullOfOrNull(String::toDoubleOrNull)
                    ?: temperatures.maxOrNull(),
            )
        }

    /** 중기예보는 발표일 기준 3~10일 뒤. 오전·오후가 나뉜 날은 더 나쁜 하늘과 더 높은 강수확률을 쓴다. */
    private fun summarizeMid(forecast: WeatherMidForecast): List<DailyWeather> {
        val announceDate = parseDate(forecast.announceTime.take(8)) ?: return emptyList()
        return forecast.midDays().map { day ->
            DailyWeather(
                date = announceDate.plusDays(day.offset.toLong()),
                sky = listOfNotNull(
                    WeatherSky.fromMidForecastText(day.weatherAm),
                    WeatherSky.fromMidForecastText(day.weatherPm),
                ).maxOrNull(),
                precipitationProbability = listOfNotNull(day.rainAm, day.rainPm).maxOrNull(),
                minTemperature = day.minTemperature?.toDouble(),
                maxTemperature = day.maxTemperature?.toDouble(),
            )
        }
    }

    /** 강수(PTY≠0)가 연속된 시간을 하나의 구간으로 묶는다. */
    private fun rainWindows(rows: List<WeatherShortForecast>): List<RainWindow> {
        val hourly = rows.filter { it.forecastCategory == PTY }
            .mapNotNull { row ->
                val sky = WeatherSky.fromPrecipitationCode(row.forecastValue) ?: return@mapNotNull null
                val at = parseDateTime(row.forecastDate, row.forecastTime) ?: return@mapNotNull null
                at to sky
            }
            .sortedBy { it.first }

        val windows = mutableListOf<RainWindow>()
        for ((at, sky) in hourly) {
            val last = windows.lastOrNull()
            if (last != null && last.end == at) {
                windows[windows.lastIndex] = last.copy(end = at.plusHours(1), sky = maxOf(last.sky, sky))
            } else {
                windows += RainWindow(at, at.plusHours(1), sky)
            }
        }
        return windows
    }

    private fun List<WeatherShortForecast>.categoryValues(category: String): List<String> =
        filter { it.forecastCategory == category }.map { it.forecastValue }

    /** 가장 자주 나온 하늘 상태. 빈도가 같으면 더 나쁜 쪽. */
    private fun List<WeatherSky>.mostFrequentWorst(): WeatherSky? =
        groupingBy { it }.eachCount().entries
            .maxWithOrNull(compareBy<Map.Entry<WeatherSky, Int>> { it.value }.thenBy { it.key })
            ?.key

    private fun hourOf(row: WeatherShortForecast): Int? = row.forecastTime.take(2).toIntOrNull()

    private fun parseDate(value: String): LocalDate? = runCatching { LocalDate.parse(value, BASIC) }.getOrNull()

    private fun parseDateTime(date: String, time: String): LocalDateTime? {
        val day = parseDate(date) ?: return null
        val hour = time.take(2).toIntOrNull()?.takeIf { it in 0..23 } ?: return null
        return day.atTime(LocalTime.of(hour, 0))
    }

    private data class MidDay(
        val offset: Int,
        val weatherAm: String?,
        val weatherPm: String?,
        val rainAm: Int?,
        val rainPm: Int?,
        val minTemperature: Int?,
        val maxTemperature: Int?,
    )

    private fun WeatherMidForecast.midDays(): List<MidDay> = listOf(
        MidDay(3, weatherDay3Am, weatherDay3Pm, rainProbabilityDay3Am, rainProbabilityDay3Pm, temperatureMinDay3, temperatureMaxDay3),
        MidDay(4, weatherDay4Am, weatherDay4Pm, rainProbabilityDay4Am, rainProbabilityDay4Pm, temperatureMinDay4, temperatureMaxDay4),
        MidDay(5, weatherDay5Am, weatherDay5Pm, rainProbabilityDay5Am, rainProbabilityDay5Pm, temperatureMinDay5, temperatureMaxDay5),
        MidDay(6, weatherDay6Am, weatherDay6Pm, rainProbabilityDay6Am, rainProbabilityDay6Pm, temperatureMinDay6, temperatureMaxDay6),
        MidDay(7, weatherDay7Am, weatherDay7Pm, rainProbabilityDay7Am, rainProbabilityDay7Pm, temperatureMinDay7, temperatureMaxDay7),
        MidDay(8, weatherDay8, null, rainProbabilityDay8, null, temperatureMinDay8, temperatureMaxDay8),
        MidDay(9, weatherDay9, null, rainProbabilityDay9, null, temperatureMinDay9, temperatureMaxDay9),
        MidDay(10, weatherDay10, null, rainProbabilityDay10, null, temperatureMinDay10, temperatureMaxDay10),
    )

    companion object {
        private val KST: ZoneId = ZoneId.of("Asia/Seoul")
        private val BASIC: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE

        private const val POP = "POP"
        private const val PTY = "PTY"
        private const val SKY = "SKY"
        private const val TMP = "TMP"
        private const val TMN = "TMN"
        private const val TMX = "TMX"
        private val SHORT_CATEGORIES = listOf(POP, PTY, SKY, TMP, TMN, TMX)

        /** 하루 대표 하늘·강수확률은 방문 시간대(06~20시)로 본다. */
        private val DAYTIME_HOURS = 6..20
    }
}
