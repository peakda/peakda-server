package com.peakda.server.infrastructure.scheduler.kma

import com.peakda.server.domain.seasonal.application.BloomObservationSyncService
import com.peakda.server.infrastructure.external.kma.flower.FlowerObservationClient
import com.peakda.server.infrastructure.scheduler.JobLogger
import com.peakda.server.infrastructure.scheduler.ManualTriggerableJob
import com.peakda.server.infrastructure.scheduler.SchedulerProperties
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 비공식 웹페이지 엔드포인트의 변경이나 일시 오류가 전체 동기화를 막지 않도록
 * 수종 목록과 장소 상세의 실패를 각각 격리하고, 성공한 현재 시즌 관측만 축적한다.
 */
@Component
class FlowerObservationSyncJob(
    private val client: FlowerObservationClient,
    private val syncService: BloomObservationSyncService,
    private val props: SchedulerProperties,
    private val jobLogger: JobLogger,
) : ManualTriggerableJob {
    override val jobName: String
        get() = JOB_NAME

    @Scheduled(cron = "\${external.scheduler.kma.flower-observation.cron}", zone = "Asia/Seoul")
    fun run() {
        jobLogger.runIfEnabled(JOB_NAME, props.enabled && props.kma.flowerObservation.enabled) { execute() }
    }

    override fun runNow() {
        jobLogger.runManually(JOB_NAME) { execute() }
    }

    private fun execute(): Map<String, Any?> {
        var processed = 0
        var placeCount = 0

        for (treeType in TREE_TYPES) {
            val places = try {
                client.getPlaces(treeType)
            } catch (e: Exception) {
                log.warn("[scheduler] job={} treeType={} status=FAILED error={}", JOB_NAME, treeType, e.message, e)
                continue
            }
            placeCount += places.size

            for (place in places) {
                try {
                    val detail = client.getObservation(treeType, place.obsPlace) ?: continue
                    processed += syncService.upsert(detail)
                } catch (e: Exception) {
                    log.warn(
                        "[scheduler] job={} treeType={} obsPlace={} status=FAILED error={}",
                        JOB_NAME,
                        treeType,
                        place.obsPlace,
                        e.message,
                        e,
                    )
                }
            }
        }

        return mapOf(
            JobLogger.KEY_PROCESSED to processed,
            "places" to placeCount,
        )
    }

    companion object {
        const val JOB_NAME = "flowerObservationSync"
        private val TREE_TYPES = 1..3
        private val log = LoggerFactory.getLogger(FlowerObservationSyncJob::class.java)
    }
}
