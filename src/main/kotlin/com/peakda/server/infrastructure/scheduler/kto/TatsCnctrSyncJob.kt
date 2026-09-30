package com.peakda.server.infrastructure.scheduler.kto

import com.peakda.server.domain.congestion.application.CongestionSyncService
import com.peakda.server.infrastructure.external.kto.tatscnctr.TatsCnctrClient
import com.peakda.server.infrastructure.external.kto.tatscnctr.TatsCnctrRegionCatalog
import com.peakda.server.infrastructure.scheduler.JobLogger
import com.peakda.server.infrastructure.scheduler.ManualTriggerableJob
import com.peakda.server.infrastructure.scheduler.SchedulerJobCursor
import com.peakda.server.infrastructure.scheduler.SchedulerProperties
import com.peakda.server.infrastructure.scheduler.runPaging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 관광지 집중률 동기화.
 *
 * 집중률 API 는 areaCd·signguCd 가 필수라 전국을 한 번에 받을 수 없다. 시군구 단위로
 * 순회하며, 한 호출이 그 시군구 관광지들의 향후 30일 예측치를 돌려준다.
 * 기준일자를 요청으로 지정하는 파라미터는 없고 응답의 baseYmd 로만 확인된다.
 *
 * 일일 호출 한도에 걸려 중간에 멈추면 다음 실행은 멈춘 시군구부터 원형으로 이어서 돈다.
 * 매번 목록 처음부터 시작하면 한도 안에 드는 앞쪽 지역만 갱신되고 뒤쪽 지역은 영영 수집되지 않는다.
 */
@Component
class TatsCnctrSyncJob(
    private val client: TatsCnctrClient,
    private val regionCatalog: TatsCnctrRegionCatalog,
    private val syncService: CongestionSyncService,
    private val props: SchedulerProperties,
    private val jobLogger: JobLogger,
    private val cursor: SchedulerJobCursor,
) : ManualTriggerableJob {
    override val jobName: String
        get() = JOB_NAME

    @Scheduled(cron = "\${external.scheduler.kto.tats-cnctr.cron}", zone = "Asia/Seoul")
    fun run() {
        jobLogger.runIfEnabled(JOB_NAME, props.enabled && props.kto.tatsCnctr.enabled) { execute() }
    }

    override fun runNow() {
        jobLogger.runManually(JOB_NAME) { execute() }
    }

    private fun execute(): Map<String, Any?> {
        val regions = regionCatalog.all
        val start = cursor.load(JOB_NAME).takeIf { it in regions.indices } ?: 0
        var processed = 0
        var totalCount = 0
        for (offset in regions.indices) {
            val index = (start + offset) % regions.size
            val region = regions[index]
            val result = runPaging(
                pageSize = PAGE_SIZE,
                maxPages = MAX_PAGES,
                extras = mapOf("areaCd" to region.areaCd, "signguCd" to region.signguCd),
                fetch = client::tatsCnctrRatedList,
                upsert = syncService::upsertPage,
            )
            processed += result.processed
            totalCount += result.totalCount
            cursor.save(JOB_NAME, (index + 1) % regions.size)
        }
        return mapOf(
            JobLogger.KEY_PROCESSED to processed,
            JobLogger.KEY_TOTAL to totalCount,
            "regions" to regions.size,
            "startIndex" to start,
        )
    }

    companion object {
        const val JOB_NAME = "tatsCnctrSync"

        /** 시군구당 약 25개 관광지 × 30일 ≈ 750행이라 1000 이면 대부분 한 번에 끝난다. */
        private const val PAGE_SIZE = 1000
        private const val MAX_PAGES = 50
    }
}
