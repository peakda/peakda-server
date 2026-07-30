package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.weather.entity.WeatherMidForecast
import com.peakda.server.domain.weather.repository.WeatherMidForecastRepository
import com.peakda.server.domain.weather.repository.WeatherShortForecastRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class ForecastTemperatureService(
    private val shortForecastRepository: WeatherShortForecastRepository,
    private val midForecastRepository: WeatherMidForecastRepository,
) {
    /**
     * [from] 이후 예보 일평균기온. 배치 실행당 한 번만 호출한다.
     * 겹치는 날짜는 더 정확한 단기예보를 우선한다.
     */
    @Transactional(readOnly = true)
    fun loadForecastTemperatures(
        gridX: Int,
        gridY: Int,
        midRegionCode: String,
        from: LocalDate,
        to: LocalDate,
    ): List<DailyTemperature> {
        val shortTemperatures = shortForecastRepository
            .findByGridXAndGridYAndForecastCategoryInAndForecastDateBetween(
                gridX = gridX,
                gridY = gridY,
                forecastCategories = SHORT_TEMPERATURE_CATEGORIES,
                forecastDateStart = from.format(DateTimeFormatter.BASIC_ISO_DATE),
                forecastDateEnd = to.format(DateTimeFormatter.BASIC_ISO_DATE),
            )
            .groupBy { forecast -> forecast.forecastDate }
            .mapNotNull { (forecastDate, forecasts) ->
                val date = parseDate(forecastDate) ?: return@mapNotNull null
                DailyTemperature(
                    observedOn = date,
                    avgTemperature = null,
                    minTemperature = forecasts.firstNotNullOfOrNull { forecast ->
                        forecast.forecastValue.toDoubleOrNull()
                            ?.takeIf { forecast.forecastCategory == MIN_TEMPERATURE_CATEGORY }
                    },
                    maxTemperature = forecasts.firstNotNullOfOrNull { forecast ->
                        forecast.forecastValue.toDoubleOrNull()
                            ?.takeIf { forecast.forecastCategory == MAX_TEMPERATURE_CATEGORY }
                    },
                )
            }
            .associateBy(DailyTemperature::observedOn)

        val merged = loadMidForecastTemperatures(midRegionCode, from, to)
            .associateBy(DailyTemperature::observedOn)
            .toMutableMap()
        // 같은 날짜의 중기예보보다 예측 시점이 가까운 단기예보를 신뢰한다.
        merged.putAll(shortTemperatures)

        return merged.values.sortedBy(DailyTemperature::observedOn)
    }

    private fun loadMidForecastTemperatures(
        regionCode: String,
        from: LocalDate,
        to: LocalDate,
    ): List<DailyTemperature> {
        val forecast = midForecastRepository.findFirstByRegionCodeOrderByAnnounceTimeDesc(regionCode)
            ?: return emptyList()
        val announceDate = parseDate(forecast.announceTime.take(8)) ?: return emptyList()

        return forecast.dailyTemperaturePairs().mapIndexedNotNull { index, (minimum, maximum) ->
            val date = announceDate.plusDays((index + MID_FORECAST_FIRST_DAY).toLong())
            if (date < from || date > to) return@mapIndexedNotNull null
            // 최저·최고가 모두 없으면 확보한 예보일로 셀 수 없으므로 결과에서 제외한다.
            if (minimum == null && maximum == null) return@mapIndexedNotNull null

            DailyTemperature(
                observedOn = date,
                avgTemperature = null,
                minTemperature = minimum?.toDouble(),
                maxTemperature = maximum?.toDouble(),
            )
        }
    }

    private fun WeatherMidForecast.dailyTemperaturePairs(): List<Pair<Int?, Int?>> = listOf(
        temperatureMinDay3 to temperatureMaxDay3,
        temperatureMinDay4 to temperatureMaxDay4,
        temperatureMinDay5 to temperatureMaxDay5,
        temperatureMinDay6 to temperatureMaxDay6,
        temperatureMinDay7 to temperatureMaxDay7,
        temperatureMinDay8 to temperatureMaxDay8,
        temperatureMinDay9 to temperatureMaxDay9,
        temperatureMinDay10 to temperatureMaxDay10,
    )

    private fun parseDate(value: String): LocalDate? =
        runCatching { LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE) }.getOrNull()

    companion object {
        private const val MIN_TEMPERATURE_CATEGORY = "TMN"
        private const val MAX_TEMPERATURE_CATEGORY = "TMX"
        private const val MID_FORECAST_FIRST_DAY = 3
        private val SHORT_TEMPERATURE_CATEGORIES = listOf(
            MIN_TEMPERATURE_CATEGORY,
            MAX_TEMPERATURE_CATEGORY,
        )
    }
}
