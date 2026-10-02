package com.peakda.server.infrastructure.scheduler.kto

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.domain.attraction.application.AttractionOperatingInfoSyncService
import com.peakda.server.infrastructure.external.common.ExternalApiException
import com.peakda.server.infrastructure.external.kto.korservice.KorServiceClient
import com.peakda.server.infrastructure.scheduler.JobLogger
import com.peakda.server.infrastructure.scheduler.ManualTriggerableJob
import com.peakda.server.infrastructure.scheduler.SchedulerProperties
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 스팟 상세 운영 정보(운영 시간·쉬는 날·입장료·주차)를 관광공사 상세 API 로 채운다.
 *
 * 관광지 소개정보(detailIntro2)에는 입장료가 없어 반복정보(detailInfo2)까지 명소당 2회 호출한다.
 * 국문 관광정보는 하루 1만 회 한도라 실행마다 [SchedulerProperties.OperatingInfoJobProps.maxAttractions] 건씩 나눠 채운다.
 * 받은 명소는 바로 저장하므로 한도·장애로 멈춰도 다음 실행이 남은 명소부터 이어 간다.
 */
@Component
class AttractionOperatingInfoSyncJob(
    private val client: KorServiceClient,
    private val syncService: AttractionOperatingInfoSyncService,
    private val props: SchedulerProperties,
    private val jobLogger: JobLogger,
) : ManualTriggerableJob {
    override val jobName: String
        get() = JOB_NAME

    @Scheduled(cron = "\${external.scheduler.kto.operating-info.cron}", zone = "Asia/Seoul")
    fun run() {
        jobLogger.runIfEnabled(JOB_NAME, props.enabled && props.kto.operatingInfo.enabled) { execute() }
    }

    override fun runNow() {
        jobLogger.runManually(JOB_NAME) { execute() }
    }

    private fun execute(): Map<String, Any?> {
        val targets = syncService.findTargets(props.kto.operatingInfo.maxAttractions)
        var processed = 0
        var failed = 0
        for (target in targets) {
            val params = mapOf("contentId" to target.tourApiContentId, "contentTypeId" to target.contentTypeCode)
            try {
                val intro = client.detailIntro(params).items.firstOrNull()
                val infos = client.detailInfo(params).items
                processed += syncService.save(target, intro, infos)
            } catch (e: ExternalApiException) {
                // 한도 초과·인증 실패·서킷 열림은 남은 명소도 같은 이유로 실패하므로 멈추고, 다음 실행이 이어서 받는다.
                if (e.errorCode in STOP_ERRORS) throw e
                log.warn("[attractionOperatingInfoSync] skipped contentId={} error={}", target.tourApiContentId, e.message)
                failed++
            }
        }
        return mapOf(
            JobLogger.KEY_PROCESSED to processed,
            JobLogger.KEY_TOTAL to targets.size,
            "failed" to failed,
        )
    }

    companion object {
        const val JOB_NAME = "attractionOperatingInfoSync"
        private val STOP_ERRORS = setOf(
            ErrorCode.EXTERNAL_API_QUOTA_EXCEEDED,
            ErrorCode.EXTERNAL_API_AUTH_FAILED,
            ErrorCode.EXTERNAL_API_UNAVAILABLE,
        )
        private val log = LoggerFactory.getLogger(AttractionOperatingInfoSyncJob::class.java)
    }
}
