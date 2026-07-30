package com.peakda.server.infrastructure.scheduler.seasonal

import com.peakda.server.domain.festival.repository.FestivalRepository
import com.peakda.server.domain.seasonal.application.BloomEstimateService
import com.peakda.server.domain.seasonal.application.AttractionStationMappingService
import com.peakda.server.domain.seasonal.application.DailyTemperature
import com.peakda.server.domain.seasonal.application.ForecastTemperatureService
import com.peakda.server.domain.seasonal.application.GddAccumulationService
import com.peakda.server.domain.seasonal.application.GddAccumulator
import com.peakda.server.domain.seasonal.application.GddProjector
import com.peakda.server.domain.seasonal.application.GddSnapshot
import com.peakda.server.domain.seasonal.application.ObservationSnapshot
import com.peakda.server.domain.seasonal.application.ObservationSnapshotService
import com.peakda.server.domain.seasonal.application.resolveAccumulationStart as resolveBloomAccumulationStart
import com.peakda.server.domain.seasonal.application.resolveForecastStart as resolveBloomForecastStart
import com.peakda.server.domain.seasonal.application.estimator.GddEstimatorProperties
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.repository.AttractionBloomRepository
import com.peakda.server.infrastructure.scheduler.JobLogger
import com.peakda.server.infrastructure.scheduler.ManualTriggerableJob
import com.peakda.server.infrastructure.scheduler.SchedulerProperties
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneId

/**
 * 명소×카테고리 개화 상태 추정 잡. 태깅된 [AttractionBloom] 을 입력으로 추정기·융합을 돌려 산출일(today) 기준 상태를 적재한다.
 *
 * 좌표 보유 축제를 한 번만 로드해 모든 추정에 공유하고, 카테고리별로 명소 id 를 페이지 단위로 순회하며 페이지마다 커밋한다.
 */
@Component
class BloomEstimateJob(
    private val attractionBloomRepository: AttractionBloomRepository,
    private val festivalRepository: FestivalRepository,
    private val estimateService: BloomEstimateService,
    private val mappingService: AttractionStationMappingService,
    private val gddAccumulationService: GddAccumulationService,
    private val forecastTemperatureService: ForecastTemperatureService,
    private val observationSnapshotService: ObservationSnapshotService,
    private val gddProperties: GddEstimatorProperties,
    private val props: SchedulerProperties,
    private val jobLogger: JobLogger,
) : ManualTriggerableJob {
    override val jobName: String
        get() = JOB_NAME

    @Scheduled(cron = "\${external.scheduler.seasonal.bloom-estimate.cron}", zone = "Asia/Seoul")
    fun run() {
        jobLogger.runIfEnabled(JOB_NAME, props.enabled && props.seasonal.bloomEstimate.enabled) { execute() }
    }

    override fun runNow() {
        jobLogger.runManually(JOB_NAME) { execute() }
    }

    private fun execute(): Map<String, Any?> {
        val today = LocalDate.now(KST)
        val festivals = festivalRepository.findByLatitudeIsNotNullAndLongitudeIsNotNull()
        val stationByAttraction = mappingService.findStationByAttraction()
        // 관측은 13개 안팎이지만 배치 전역에서 한 번만 읽어 페이지별 조회를 막는다.
        val observationsByStation = observationSnapshotService.findByStationAndCategory(today.year)
        val defaultStationId = gddProperties.defaultStationId
        val stationIds = (stationByAttraction.values + defaultStationId).filter { it.isNotBlank() }.toSet()
        // 필요한 모든 지점의 올해 관측을 한 번만 읽어 명소 수에 비례한 조회를 막는다.
        val temperaturesByStation =
            if (gddProperties.enabled && stationIds.isNotEmpty()) {
                gddAccumulationService.loadDailyTemperatures(
                    stationIds,
                    today.withDayOfYear(1),
                    today,
                )
            } else {
                emptyMap()
            }
        val defaultTemperatures = temperaturesByStation[defaultStationId]
        // 예보 수집은 기본 격자 하나를 유지하며 기본 지점의 실측 다음날부터 잇는다.
        val forecasts =
            if (defaultTemperatures != null && gddProperties.defaultMidRegionCode.isNotBlank()) {
                forecastTemperatureService.loadForecastTemperatures(
                    gridX = gddProperties.defaultGridX,
                    gridY = gddProperties.defaultGridY,
                    midRegionCode = gddProperties.defaultMidRegionCode,
                    from = resolveForecastStart(defaultTemperatures, today),
                    to = today.plusDays(gddProperties.forecastHorizonDays),
                )
            } else {
                emptyList()
            }
        var estimates = 0
        val gddStationIds = mutableSetOf<String>()
        for (category in BloomCategory.entries) {
            // 지점×카테고리 단위로만 누적해 계산량이 명소 수에 비례하지 않도록 한다.
            val snapshotByStation = gddProperties.thresholds[category.name]?.let { threshold ->
                val accumulationStart = resolveAccumulationStart(
                    today.year,
                    threshold.accumulationStartMonth,
                    threshold.accumulationStartDay,
                )
                temperaturesByStation.mapValues { (stationId, dailyTemperatures) ->
                    val scoped = dailyTemperatures.filter { !it.observedOn.isBefore(accumulationStart) }
                    val accumulated = GddAccumulator.accumulate(scoped, threshold.tBase)
                    GddSnapshot(
                        stationId = stationId,
                        accumulated = accumulated,
                        projectedPeakStartDate = GddProjector.projectThresholdDate(
                            accumulated = accumulated,
                            forecasts = forecasts,
                            tBase = threshold.tBase,
                            threshold = threshold.peak,
                        ),
                        projectedPeakEndDate = GddProjector.projectThresholdDate(
                            accumulated = accumulated,
                            forecasts = forecasts,
                            tBase = threshold.tBase,
                            threshold = threshold.end,
                        ),
                    )
                }
            } ?: emptyMap()
            gddStationIds += snapshotByStation.keys
            var page = 0
            while (true) {
                val slice = attractionBloomRepository
                    .findDistinctAttractionIdsByBloomCategory(category, PageRequest.of(page, PAGE_SIZE))
                if (slice.isEmpty) break
                val gddByAttraction = resolveGddByAttraction(
                    attractionIds = slice.content,
                    stationByAttraction = stationByAttraction,
                    defaultStationId = defaultStationId,
                    snapshotByStation = snapshotByStation,
                )
                val observations = resolveObservationByAttraction(
                    attractionIds = slice.content,
                    stationByAttraction = stationByAttraction,
                    category = category,
                    observationsByStation = observationsByStation,
                )
                estimates += estimateService.estimatePage(
                    attractionIds = slice.content,
                    category = category,
                    baseDate = today,
                    festivals = festivals,
                    gdd = gddByAttraction,
                    observations = observations,
                )
                if (!slice.hasNext()) break
                page++
            }
        }
        return mapOf(
            JobLogger.KEY_PROCESSED to estimates,
            "festivals" to festivals.size,
            "gddStations" to gddStationIds.size,
            "mappedAttractions" to stationByAttraction.size,
            "forecastDays" to forecasts.size,
            "observationStations" to observationsByStation.size,
        )
    }

    companion object {
        const val JOB_NAME = "bloomEstimate"
        private const val PAGE_SIZE = 500
        private val KST = ZoneId.of("Asia/Seoul")

        /**
         * 예보를 이어 붙일 시작일. 실측 마지막 관측일 다음날부터 쓴다.
         *
         * 실측이 밀려 공백이 생기면 그 구간을 예보로 메우고(단기예보는 지난 날짜분도 남아 있다),
         * 실측에 이미 오늘이 있으면 같은 날을 두 번 누적하지 않는다.
         */
        internal fun resolveForecastStart(
            observed: List<DailyTemperature>,
            today: LocalDate,
        ): LocalDate = resolveBloomForecastStart(observed, today)

        internal fun resolveAccumulationStart(year: Int, month: Int, day: Int): LocalDate =
            resolveBloomAccumulationStart(year, month, day)

        internal fun resolveGddByAttraction(
            attractionIds: List<Long>,
            stationByAttraction: Map<Long, String>,
            defaultStationId: String,
            snapshotByStation: Map<String, GddSnapshot>,
        ): Map<Long, GddSnapshot> = attractionIds.mapNotNull { attractionId ->
            val stationId = stationByAttraction[attractionId] ?: defaultStationId
            snapshotByStation[stationId]?.let { snapshot -> attractionId to snapshot }
        }.toMap()

        internal fun resolveObservationByAttraction(
            attractionIds: List<Long>,
            stationByAttraction: Map<Long, String>,
            category: BloomCategory,
            observationsByStation: Map<String, Map<BloomCategory, ObservationSnapshot>>,
        ): Map<Long, ObservationSnapshot> = attractionIds.mapNotNull { attractionId ->
            val stationId = stationByAttraction[attractionId] ?: return@mapNotNull null
            observationsByStation[stationId]?.get(category)?.let { snapshot -> attractionId to snapshot }
        }.toMap()
    }
}
