package com.peakda.server.infrastructure.scheduler.kto

import com.peakda.server.domain.congestion.application.CongestionSyncService
import com.peakda.server.domain.congestion.repository.CongestionRepository
import com.peakda.server.infrastructure.external.kto.tatscnctr.TatsCnctrClient
import com.peakda.server.infrastructure.external.kto.tatscnctr.TatsCnctrRegionCatalog
import com.peakda.server.infrastructure.external.kto.tatscnctr.response.CnctrRateItem
import com.peakda.server.infrastructure.scheduler.SchedulerProperties
import com.peakda.server.infrastructure.scheduler.ktoFixture
import com.peakda.server.infrastructure.scheduler.testErrorDecoder
import com.peakda.server.infrastructure.scheduler.testJobLogger
import com.peakda.server.infrastructure.scheduler.testObjectMapper
import com.peakda.server.infrastructure.scheduler.testResilience
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess

class TatsCnctrSyncJobTest {
    private val fixture = ktoFixture("https://example.test/tats", "TatsCnctrRateService") {
        TatsCnctrClient(it, testObjectMapper, testErrorDecoder, testResilience)
    }
    private val syncService = RecordingCongestionSync()

    @Test
    fun `run 시 시군구마다 tatsCnctrRatedList를 호출해 sync service에 페이지를 전달한다`() {
        fixture.server.expect(requestTo(uriFor("11", "11110")))
            .andRespond(withSuccess(successJson("종로구 관광지"), MediaType.APPLICATION_JSON))
        fixture.server.expect(requestTo(uriFor("26", "26110")))
            .andRespond(withSuccess(successJson("중구 관광지"), MediaType.APPLICATION_JSON))

        val job = TatsCnctrSyncJob(fixture.client, twoRegionCatalog(), syncService, enabled(true), testJobLogger())
        job.run()

        fixture.server.verify()
        assertThat(syncService.pages.flatten())
            .extracting<String> { it.tAtsNm }
            .containsExactly("종로구 관광지", "중구 관광지")
    }

    @Test
    fun `enabled=false 이면 client와 sync service 모두 호출하지 않는다`() {
        val job = TatsCnctrSyncJob(fixture.client, twoRegionCatalog(), syncService, enabled(false), testJobLogger())

        job.run()

        fixture.server.verify()
        assertThat(syncService.pages).isEmpty()
    }

    private fun twoRegionCatalog() = TatsCnctrRegionCatalog(ByteArrayResource(TWO_REGION_CSV.toByteArray()))

    private fun enabled(jobEnabled: Boolean) = SchedulerProperties(
        enabled = true,
        kto = SchedulerProperties.KtoSchedulerProps(
            tatsCnctr = SchedulerProperties.JobProps(cron = "* * * * * *", enabled = jobEnabled),
        ),
    )

    private class RecordingCongestionSync :
        CongestionSyncService(Mockito.mock(CongestionRepository::class.java)) {
        val pages = mutableListOf<List<CnctrRateItem>>()
        override fun upsertPage(items: List<CnctrRateItem>): Int {
            pages += items.toList(); return items.size
        }
    }

    companion object {
        private val TWO_REGION_CSV = """
            areaCd,areaNm,sigunguCd,sigunguNm
            11,서울특별시,11110,종로구
            26,부산광역시,26110,중구
        """.trimIndent()

        private fun uriFor(areaCd: String, signguCd: String) =
            "https://example.test/tats/tatsCnctrRatedList" +
                "?numOfRows=100&pageNo=1&areaCd=$areaCd&signguCd=$signguCd" +
                "&serviceKey=test-key&MobileOS=ETC&MobileApp=peakda-test&_type=json"

        private fun successJson(attractionName: String) = """
            { "response": { "header": { "resultCode": "0000", "resultMsg": "OK" },
              "body": { "items": { "item": [ { "baseYmd": "20260512", "tAtsNm": "$attractionName", "cnctrRate": "45" } ] },
                "totalCount": 1 } } }
        """.trimIndent()
    }
}
