package com.peakda.server.infrastructure.scheduler.kma

import com.peakda.server.domain.weather.application.AttractionForecastAreaMappingService
import com.peakda.server.domain.weather.application.WeatherShortForecastSyncService
import com.peakda.server.infrastructure.external.kma.vilagefcst.VilageFcstClient
import com.peakda.server.infrastructure.scheduler.JobLogger
import com.peakda.server.infrastructure.scheduler.ManualTriggerableJob
import com.peakda.server.infrastructure.scheduler.SchedulerProperties
import com.peakda.server.infrastructure.scheduler.SchedulerTime.HH00
import com.peakda.server.infrastructure.scheduler.SchedulerTime.KST
import com.peakda.server.infrastructure.scheduler.SchedulerTime.YMD
import com.peakda.server.infrastructure.scheduler.runPaging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class VilageFcstSyncJob(
    private val client: VilageFcstClient,
    private val syncService: WeatherShortForecastSyncService,
    private val forecastAreaService: AttractionForecastAreaMappingService,
    private val props: SchedulerProperties,
    private val jobLogger: JobLogger,
) : ManualTriggerableJob {
    override val jobName: String
        get() = JOB_NAME

    @Scheduled(cron = "\${external.scheduler.kma.vilage-fcst.cron}", zone = "Asia/Seoul")
    fun run() {
        jobLogger.runIfEnabled(JOB_NAME, props.enabled && props.kma.vilageFcst.enabled) { execute() }
    }

    override fun runNow() {
        jobLogger.runManually(JOB_NAME) { execute() }
    }

    private fun execute(): Map<String, Any?> {
        val grids = collectionGrids()
        val base = latestVilageBase()
        val baseDate = base.format(YMD)
        val baseTime = base.format(HH00)
        var processed = 0
        for (grid in grids) {
            val result = runPaging(
                pageSize = PAGE_SIZE,
                maxPages = MAX_PAGES,
                extras = mapOf("base_date" to baseDate, "base_time" to baseTime, "nx" to grid.nx, "ny" to grid.ny),
                fetch = client::getVilageFcst,
                upsert = syncService::upsertPage,
            )
            processed += result.processed
        }
        return mapOf(
            JobLogger.KEY_PROCESSED to processed,
            "grids" to grids.size,
            "baseDate" to baseDate,
            "baseTime" to baseTime,
        )
    }

    /**
     * 설정 격자(광역시 대표 지점 — 개화 GDD 계산용)에 계절 명소가 모인 격자를 더한다.
     * 격자 하나가 하루 8회 호출이므로 명소 격자 수는 [SchedulerProperties.VilageFcstJobProps.maxAttractionGrids] 로 제한한다.
     */
    private fun collectionGrids(): List<SchedulerProperties.VilageFcstJobProps.Grid> {
        val configured = props.kma.vilageFcst.grids.ifEmpty { listOf(DEFAULT_GRID) }
        val known = configured.map { it.nx to it.ny }.toMutableSet()
        val attractionGrids = forecastAreaService
            .findCollectionGrids(props.kma.vilageFcst.maxAttractionGrids)
            .filter { known.add(it.gridX to it.gridY) }
            .map { SchedulerProperties.VilageFcstJobProps.Grid("attraction", it.gridX, it.gridY) }
        return configured + attractionGrids
    }

    private fun latestVilageBase(): LocalDateTime {
        return resolveLatestVilageBase(LocalDateTime.now(KST))
    }

    companion object {
        const val JOB_NAME = "vilageFcstSync"
        private const val PAGE_SIZE = 1000
        private const val MAX_PAGES = 20
        private val DEFAULT_GRID = SchedulerProperties.VilageFcstJobProps.Grid("서울", 60, 127)
        private val BASE_HOURS = listOf(2, 5, 8, 11, 14, 17, 20, 23)

        internal fun resolveLatestVilageBase(now: LocalDateTime): LocalDateTime {
            val availableAt = now.minusMinutes(10)
            val baseHour = BASE_HOURS.lastOrNull { it <= availableAt.hour }

            return if (baseHour == null) {
                availableAt.toLocalDate()
                    .minusDays(1)
                    .atTime(BASE_HOURS.last(), 0)
            } else {
                availableAt.toLocalDate()
                    .atTime(baseHour, 0)
            }
        }
    }
}
