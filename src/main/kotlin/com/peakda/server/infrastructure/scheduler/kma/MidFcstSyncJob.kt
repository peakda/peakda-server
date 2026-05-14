package com.peakda.server.infrastructure.scheduler.kma

import com.peakda.server.domain.weather.application.WeatherMidForecastSyncService
import com.peakda.server.infrastructure.external.kma.midfcst.MidFcstClient
import com.peakda.server.infrastructure.external.kma.midfcst.MidRegionCode
import com.peakda.server.infrastructure.scheduler.JobLogger
import com.peakda.server.infrastructure.scheduler.SchedulerProperties
import com.peakda.server.infrastructure.scheduler.SchedulerTime.KST
import com.peakda.server.infrastructure.scheduler.SchedulerTime.YMD
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class MidFcstSyncJob(
    private val client: MidFcstClient,
    private val syncService: WeatherMidForecastSyncService,
    private val props: SchedulerProperties,
    private val jobLogger: JobLogger,
) {
    @Scheduled(cron = "\${external.scheduler.kma.mid-fcst.cron}", zone = "Asia/Seoul")
    fun run() {
        jobLogger.runIfEnabled(JOB_NAME, props.enabled && props.kma.midFcst.enabled) {
            val announcementTime = latestAnnouncementTime()
            var processed = 0
            for (region in MidRegionCode.entries) {
                processed += syncRegion(region, announcementTime)
            }
            mapOf(
                JobLogger.KEY_PROCESSED to processed,
                "regions" to MidRegionCode.entries.size,
                "tmFc" to announcementTime,
            )
        }
    }

    private fun syncRegion(region: MidRegionCode, announcementTime: String): Int {
        var processed = 0
        client.getMidLandFcst(landForecastParams(region, announcementTime)).item.firstOrNull()?.let {
            processed += syncService.upsertLandForecast(region.name, region.landRegId, announcementTime, it)
        }
        client.getMidTa(temperatureForecastParams(region, announcementTime)).item.firstOrNull()?.let {
            processed += syncService.upsertTemperatureForecast(region.name, region.temperatureRegId, announcementTime, it)
        }
        return processed
    }

    private fun landForecastParams(region: MidRegionCode, announcementTime: String) =
        mapOf("numOfRows" to 10, "pageNo" to 1, "regId" to region.landRegId, "tmFc" to announcementTime)

    private fun temperatureForecastParams(region: MidRegionCode, announcementTime: String) =
        mapOf("numOfRows" to 10, "pageNo" to 1, "regId" to region.temperatureRegId, "tmFc" to announcementTime)

    private fun latestAnnouncementTime(): String {
        val now = LocalDateTime.now(KST)
        val day = now.toLocalDate()
        return when {
            now.hour >= 18 -> day.format(YMD) + "1800"
            now.hour >= 6 -> day.format(YMD) + "0600"
            else -> day.minusDays(1).format(YMD) + "1800"
        }
    }

    companion object {
        const val JOB_NAME = "midFcstSync"
    }
}
