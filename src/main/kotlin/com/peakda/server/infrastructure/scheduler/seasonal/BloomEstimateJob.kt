package com.peakda.server.infrastructure.scheduler.seasonal

import com.peakda.server.domain.festival.repository.FestivalRepository
import com.peakda.server.domain.seasonal.application.BloomEstimateService
import com.peakda.server.domain.seasonal.application.DailyTemperature
import com.peakda.server.domain.seasonal.application.ForecastTemperatureService
import com.peakda.server.domain.seasonal.application.GddAccumulationService
import com.peakda.server.domain.seasonal.application.GddAccumulator
import com.peakda.server.domain.seasonal.application.GddProjector
import com.peakda.server.domain.seasonal.application.GddSnapshot
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
    private val gddAccumulationService: GddAccumulationService,
    private val forecastTemperatureService: ForecastTemperatureService,
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
        val stationId = gddProperties.defaultStationId
        // 기본 지점의 올해 관측을 한 번만 읽어 모든 명소와 카테고리에서 재사용한다.
        val temperatures =
            if (gddProperties.enabled && stationId.isNotBlank()) {
                gddAccumulationService.loadDailyTemperatures(
                    listOf(stationId),
                    today.withDayOfYear(1),
                    today,
                )[stationId]
            } else {
                null
            }
        // 예보도 한 번만 읽어 종별 임계치 계산에 공유해야 명소 수에 비례한 조회를 막을 수 있다.
        val forecasts =
            if (temperatures != null && gddProperties.defaultMidRegionCode.isNotBlank()) {
                forecastTemperatureService.loadForecastTemperatures(
                    gridX = gddProperties.defaultGridX,
                    gridY = gddProperties.defaultGridY,
                    midRegionCode = gddProperties.defaultMidRegionCode,
                    from = resolveForecastStart(temperatures, today),
                    to = today.plusDays(gddProperties.forecastHorizonDays),
                )
            } else {
                emptyList()
            }
        var estimates = 0
        for (category in BloomCategory.entries) {
            // 종별 기준온도만 바꿔 같은 관측을 누적하므로 카테고리마다 재조회하지 않는다.
            val gdd = temperatures?.let { dailyTemperatures ->
                gddProperties.thresholds[category.name]?.let { threshold ->
                    val accumulated = GddAccumulator.accumulate(dailyTemperatures, threshold.tBase)
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
            }
            var page = 0
            while (true) {
                val slice = attractionBloomRepository
                    .findDistinctAttractionIdsByBloomCategory(category, PageRequest.of(page, PAGE_SIZE))
                if (slice.isEmpty) break
                estimates += estimateService.estimatePage(slice.content, category, today, festivals, gdd)
                if (!slice.hasNext()) break
                page++
            }
        }
        return mapOf(
            JobLogger.KEY_PROCESSED to estimates,
            "festivals" to festivals.size,
            "gddStation" to stationId.takeIf { temperatures != null },
            "forecastDays" to forecasts.size,
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
        ): LocalDate = observed.maxOfOrNull { it.observedOn }?.plusDays(1) ?: today
    }
}
