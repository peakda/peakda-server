package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.festival.repository.FestivalRepository
import com.peakda.server.domain.seasonal.application.estimator.GddEstimatorProperties
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.seasonal.presentation.response.BloomCalendarResponse
import com.peakda.server.domain.seasonal.presentation.response.BloomCalendarResponse.BloomCalendarDay
import com.peakda.server.domain.spot.exception.AttractionNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId

/**
 * 단일 명소×카테고리의 향후 [BloomCalendarProperties.calendarHorizonDays] 일 예상 만개 캘린더를 온디맨드로 시뮬레이션한다.
 *
 * 저장하지 않으며(스팟 상세 1건 조회용), 각 일자를 융합 추정기로 돌려 일별 상태 타임라인과 대표 절정 구간을 만든다.
 */
@Service
class BloomCalendarService(
    private val attractionRepository: AttractionRepository,
    private val festivalRepository: FestivalRepository,
    private val fusionService: BloomStatusFusionService,
    private val properties: BloomCalendarProperties,
    private val mappingService: AttractionStationMappingService,
    private val gddAccumulationService: GddAccumulationService,
    private val forecastTemperatureService: ForecastTemperatureService,
    private val gddProperties: GddEstimatorProperties,
    private val observationSnapshotService: ObservationSnapshotService,
) {
    @Transactional(readOnly = true)
    fun getCalendar(attractionId: Long, category: BloomCategory): BloomCalendarResponse {
        val attraction = attractionRepository.findById(attractionId)
            .orElseThrow { AttractionNotFoundException() }
        val festivals = festivalRepository.findByLatitudeIsNotNullAndLongitudeIsNotNull()
        val today = LocalDate.now(KST)
        val dates = (0 until properties.calendarHorizonDays).map { offset -> today.plusDays(offset) }
        val mappedStationId = mappingService.findStationId(attractionId)
        val gddByDate = resolveGddByDate(mappedStationId, category, today, dates)
        val observation = mappedStationId
            ?.let { stationId ->
                observationSnapshotService.findByStationAndCategory(today.year)[stationId]?.get(category)
            }

        var representative: BloomEstimation? = null
        val days = dates.mapIndexed { offset, date ->
            val estimation = fusionService.fuse(
                BloomEstimationContext(
                    attraction = attraction,
                    category = category,
                    baseDate = date,
                    festivals = festivals,
                    gdd = gddByDate[date],
                    observation = observation,
                ),
            )
            if (offset == 0) representative = estimation
            BloomCalendarDay(date = date, status = estimation?.status ?: BloomStatus.PREPARING)
        }

        return BloomCalendarResponse(
            attractionId = attractionId,
            category = category,
            displayName = category.displayName,
            peakStartDate = representative?.peakStartDate,
            peakEndDate = representative?.peakEndDate,
            peakDurationDays = representative?.peakDurationDays,
            days = days,
        )
    }

    private fun resolveGddByDate(
        mappedStationId: String?,
        category: BloomCategory,
        today: LocalDate,
        dates: List<LocalDate>,
    ): Map<LocalDate, GddSnapshot> {
        if (!gddProperties.enabled) return emptyMap()
        val threshold = gddProperties.thresholds[category.name] ?: return emptyMap()
        val stationId = mappedStationId
            ?: gddProperties.defaultStationId.takeIf { it.isNotBlank() }
            ?: return emptyMap()

        val accumulationStart = resolveAccumulationStart(
            today.year,
            threshold.accumulationStartMonth,
            threshold.accumulationStartDay,
        )
        val observed = gddAccumulationService
            .loadDailyTemperatures(setOf(stationId), today.withDayOfYear(1), today)[stationId]
            .orEmpty()
        // 예보는 실측 누적 뒤에 잇는 값이라 단독으로는 의미가 없다. 실측이 없으면 GDD를 만들지 않고
        // 달력·축제 신호로 넘긴다. 배치(BloomEstimateJob)도 같은 이유로 실측이 없으면 예보를 읽지 않는다.
        if (observed.isEmpty()) return emptyMap()
        val baseAccumulated = GddAccumulator.accumulate(
            observed.filter { temperature -> !temperature.observedOn.isBefore(accumulationStart) },
            threshold.tBase,
        )
        val forecasts =
            if (gddProperties.defaultMidRegionCode.isNotBlank()) {
                forecastTemperatureService.loadForecastTemperatures(
                    gridX = gddProperties.defaultGridX,
                    gridY = gddProperties.defaultGridY,
                    midRegionCode = gddProperties.defaultMidRegionCode,
                    from = resolveForecastStart(observed, today),
                    to = today.plusDays(gddProperties.forecastHorizonDays),
                )
            } else {
                emptyList()
            }
        val accumulatedByDate = accumulateByDate(
            baseAccumulated = baseAccumulated,
            forecasts = forecasts,
            tBase = threshold.tBase,
            dates = dates,
        )

        return accumulatedByDate.mapValues { (date, accumulated) ->
            val remainingForecasts = forecasts.filter { forecast -> forecast.observedOn.isAfter(date) }
            GddSnapshot(
                stationId = stationId,
                accumulated = accumulated,
                projectedPeakStartDate = GddProjector.projectThresholdDate(
                    accumulated = accumulated,
                    forecasts = remainingForecasts,
                    tBase = threshold.tBase,
                    threshold = threshold.peak,
                ),
                projectedPeakEndDate = GddProjector.projectThresholdDate(
                    accumulated = accumulated,
                    forecasts = remainingForecasts,
                    tBase = threshold.tBase,
                    threshold = threshold.end,
                ),
            )
        }
    }

    companion object {
        private val KST = ZoneId.of("Asia/Seoul")
    }
}
