package com.peakda.server.infrastructure.scheduler.kto

import com.peakda.server.domain.attraction.application.AttractionThumbnailCheckService
import com.peakda.server.infrastructure.external.kto.image.KtoImageProbeClient
import com.peakda.server.infrastructure.external.kto.image.KtoImageProbeResult
import com.peakda.server.infrastructure.scheduler.JobLogger
import com.peakda.server.infrastructure.scheduler.ManualTriggerableJob
import com.peakda.server.infrastructure.scheduler.SchedulerProperties
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 명소 썸네일(firstimage2) URL 이 관광공사 이미지 서버에 실제로 있는지 확인한다.
 *
 * 없다고 확인된 썸네일은 목록 카드에서 원본(firstimage)으로 바뀐다. 판단할 수 없는 응답(타임아웃·5xx)은 저장하지 않고 다음 실행에 다시 확인한다.
 * 판단할 수 없는 응답이 [MAX_CONSECUTIVE_UNKNOWN] 번 이어지면 이미지 서버 장애로 보고 멈춘다.
 * 관광공사 이미지 서버 주소가 아닌 썸네일은 확인할 수 없으므로 확인한 것으로 저장해 매번 대상에 다시 오르지 않게 한다.
 * 확인한 명소는 바로 저장하므로 중간에 멈춰도 다음 실행이 남은 명소부터 이어 간다.
 */
@Component
class AttractionThumbnailCheckJob(
    private val client: KtoImageProbeClient,
    private val checkService: AttractionThumbnailCheckService,
    private val props: SchedulerProperties,
    private val jobLogger: JobLogger,
) : ManualTriggerableJob {
    override val jobName: String
        get() = JOB_NAME

    @Scheduled(cron = "\${external.scheduler.kto.thumbnail-check.cron}", zone = "Asia/Seoul")
    fun run() {
        jobLogger.runIfEnabled(JOB_NAME, props.enabled && props.kto.thumbnailCheck.enabled) { execute() }
    }

    override fun runNow() {
        jobLogger.runManually(JOB_NAME) { execute() }
    }

    private fun execute(): Map<String, Any?> {
        val jobProps = props.kto.thumbnailCheck
        val targets = checkService.findTargets(jobProps.maxAttractions, jobProps.recheckAfter)
        var processed = 0
        var missing = 0
        var unknown = 0
        var consecutiveUnknown = 0
        var stopped = false
        for (target in targets) {
            when (client.probe(target.thumbnailImageUrl)) {
                KtoImageProbeResult.AVAILABLE, KtoImageProbeResult.UNSUPPORTED -> checkService.save(target, missing = false)
                KtoImageProbeResult.MISSING -> {
                    checkService.save(target, missing = true)
                    missing++
                }
                KtoImageProbeResult.UNKNOWN -> {
                    unknown++
                    consecutiveUnknown++
                    if (consecutiveUnknown >= MAX_CONSECUTIVE_UNKNOWN) {
                        stopped = true
                        break
                    }
                    continue
                }
            }
            consecutiveUnknown = 0
            processed++
        }
        return mapOf(
            JobLogger.KEY_PROCESSED to processed,
            JobLogger.KEY_TOTAL to targets.size,
            "missing" to missing,
            "unknown" to unknown,
            "stopped" to stopped,
        )
    }

    companion object {
        const val JOB_NAME = "attractionThumbnailCheck"
        const val MAX_CONSECUTIVE_UNKNOWN = 20
    }
}
