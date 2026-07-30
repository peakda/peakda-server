package com.peakda.server.infrastructure.scheduler.kma

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.domain.weather.application.WeatherDailyObservationSyncService
import com.peakda.server.infrastructure.external.common.ExternalApiException
import com.peakda.server.infrastructure.external.kma.asosdaly.AsosDalyClient
import com.peakda.server.infrastructure.scheduler.JobLogger
import com.peakda.server.infrastructure.scheduler.ManualTriggerableJob
import com.peakda.server.infrastructure.scheduler.SchedulerProperties
import com.peakda.server.infrastructure.scheduler.SchedulerTime.KST
import com.peakda.server.infrastructure.scheduler.SchedulerTime.YMD
import com.peakda.server.infrastructure.scheduler.runPaging
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * 초기 지점 목록은 아직 실측 검증되지 않았으므로 개별 지점 실패는 격리하되,
 * JobLogger 의 quota skip 계약을 지키기 위해 호출 한도 초과 예외는 그대로 전파한다.
 */
@Component
class AsosDalySyncJob(
    private val client: AsosDalyClient,
    private val syncService: WeatherDailyObservationSyncService,
    private val props: SchedulerProperties,
    private val jobLogger: JobLogger,
) : ManualTriggerableJob {
    override val jobName: String
        get() = JOB_NAME

    @Scheduled(cron = "\${external.scheduler.kma.asos-daly.cron}", zone = "Asia/Seoul")
    fun run() {
        jobLogger.runIfEnabled(JOB_NAME, props.enabled && props.kma.asosDaly.enabled) { execute() }
    }

    override fun runNow() {
        jobLogger.runManually(JOB_NAME) { execute() }
    }

    private fun execute(): Map<String, Any?> {
        val yesterday = LocalDate.now(KST).minusDays(1)
        val latestByStation = syncService.findLatestObservedOnByStation()
        var processed = 0
        var syncedStations = 0
        var skippedStations = 0

        for (stationId in props.kma.asosDaly.stations) {
            val range = resolveBackfillRange(
                lastObserved = latestByStation[stationId],
                backfillFrom = props.kma.asosDaly.backfillFrom,
                yesterday = yesterday,
                maxBackfillDays = props.kma.asosDaly.maxBackfillDays,
            )
            if (range == null) {
                skippedStations++
                continue
            }

            try {
                val result = runPaging(
                    pageSize = PAGE_SIZE,
                    maxPages = MAX_PAGES,
                    extras = mapOf(
                        "dataCd" to "ASOS",
                        "dateCd" to "DAY",
                        "startDt" to range.start.format(YMD),
                        "endDt" to range.endInclusive.format(YMD),
                        "stnIds" to stationId,
                    ),
                    fetch = client::getWthrDataList,
                    upsert = syncService::upsertPage,
                )
                processed += result.processed
                syncedStations++
            } catch (e: ExternalApiException) {
                if (e.errorCode == ErrorCode.EXTERNAL_API_QUOTA_EXCEEDED) throw e
                log.warn("[scheduler] job={} stationId={} status=FAILED error={}", JOB_NAME, stationId, e.message, e)
            } catch (e: Exception) {
                log.warn("[scheduler] job={} stationId={} status=FAILED error={}", JOB_NAME, stationId, e.message, e)
            }
        }

        return mapOf(
            JobLogger.KEY_PROCESSED to processed,
            "stations" to syncedStations,
            "skipped" to skippedStations,
        )
    }

    companion object {
        const val JOB_NAME = "asosDalySync"
        private const val PAGE_SIZE = 500
        private const val MAX_PAGES = 5
        private val log = LoggerFactory.getLogger(AsosDalySyncJob::class.java)

        internal fun resolveBackfillRange(
            lastObserved: LocalDate?,
            backfillFrom: LocalDate,
            yesterday: LocalDate,
            maxBackfillDays: Long,
        ): ClosedRange<LocalDate>? {
            val start = lastObserved?.plusDays(1) ?: backfillFrom
            if (start.isAfter(yesterday)) return null

            val end = minOf(yesterday, start.plusDays(maxBackfillDays - 1))
            return start..end
        }
    }
}
