package com.peakda.server.infrastructure.scheduler.kto

import com.peakda.server.domain.attraction.application.AttractionSyncService
import com.peakda.server.domain.spot.application.AttractionSpotMaterializationService
import com.peakda.server.infrastructure.external.kto.korservice.KorServiceClient
import com.peakda.server.infrastructure.scheduler.JobLogger
import com.peakda.server.infrastructure.scheduler.ManualTriggerableJob
import com.peakda.server.infrastructure.scheduler.runPaging
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 관리자 수동 트리거로 국문 관광정보 전체를 다시 받아 명소를 upsert 한다.
 *
 * 일일 동기화([KorServiceSyncJob])는 전날 수정분만 받으므로, 새로 저장하기 시작한 컬럼
 * (법정동 코드 등)을 기존 명소 전체에 채울 때 쓴다. 약 5만 건, 1000건 페이지로 약 50회 호출한다.
 */
@Component
class AttractionFullSyncJob(
    private val client: KorServiceClient,
    private val syncService: AttractionSyncService,
    private val materializationService: AttractionSpotMaterializationService,
    private val jobLogger: JobLogger,
) : ManualTriggerableJob {
    override val jobName: String
        get() = JOB_NAME

    override fun runNow() {
        jobLogger.runManually(JOB_NAME) { execute() }
    }

    private fun execute(): Map<String, Any?> {
        val result = runPaging(
            pageSize = PAGE_SIZE,
            maxPages = MAX_PAGES,
            fetch = client::areaBasedSyncList,
            upsert = syncService::upsertPage,
        )
        if (result.processed < result.totalCount) {
            log.warn("[attractionFullSync] stopped early processed={} total={}", result.processed, result.totalCount)
        }
        val materialization = materializationService.materializeVisibleAttractions()
        return mapOf(
            JobLogger.KEY_PROCESSED to result.processed,
            JobLogger.KEY_TOTAL to result.totalCount,
            "spotProcessed" to materialization.processed,
            "spotHidden" to materialization.hidden,
            "spotShown" to materialization.shown,
        )
    }

    companion object {
        const val JOB_NAME = "attractionFullSync"
        private const val PAGE_SIZE = 1000

        /** 서버가 페이지를 100건으로 잘라도 약 5만 건을 끝까지 받을 수 있게 넉넉히 둔다. */
        private const val MAX_PAGES = 1000
        private val log = LoggerFactory.getLogger(AttractionFullSyncJob::class.java)
    }
}
