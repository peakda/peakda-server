package com.peakda.server.infrastructure.scheduler.kto

import com.peakda.server.domain.congestion.application.CongestionSyncService
import com.peakda.server.domain.congestion.repository.CongestionRepository
import com.peakda.server.infrastructure.external.kto.tatscnctr.TatsCnctrClient
import com.peakda.server.infrastructure.external.kto.tatscnctr.response.CnctrRateItem
import com.peakda.server.infrastructure.scheduler.SchedulerProperties
import com.peakda.server.infrastructure.scheduler.ktoFixture
import com.peakda.server.infrastructure.scheduler.testErrorDecoder
import com.peakda.server.infrastructure.scheduler.testJobLogger
import com.peakda.server.infrastructure.scheduler.testObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess

class TatsCnctrSyncJobTest {
    private val fixture = ktoFixture("https://example.test/tats", "TatsCnctrRateService") {
        TatsCnctrClient(it, testObjectMapper, testErrorDecoder)
    }
    private val syncService = RecordingCongestionSync()

    @Test
    fun `run 시 tatsCnctrRateList를 호출해 sync service에 페이지를 전달한다`() {
        fixture.server.expect(
            requestTo(startsWith("https://example.test/tats/tatsCnctrRateList?numOfRows=100&pageNo=1&baseYmd=")),
        ).andRespond(withSuccess(SUCCESS_JSON, MediaType.APPLICATION_JSON))

        val job = TatsCnctrSyncJob(fixture.client, syncService, enabled(true), testJobLogger())
        job.run()

        fixture.server.verify()
        assertThat(syncService.pages.flatten()).extracting<String> { it.tAtsCd }.containsExactly("T001")
    }

    @Test
    fun `enabled=false 이면 client와 sync service 모두 호출하지 않는다`() {
        val job = TatsCnctrSyncJob(fixture.client, syncService, enabled(false), testJobLogger())

        job.run()

        fixture.server.verify()
        assertThat(syncService.pages).isEmpty()
    }

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
        private val SUCCESS_JSON = """
            { "response": { "header": { "resultCode": "0000", "resultMsg": "OK" },
              "body": { "items": { "item": [ { "baseYmd": "20260512", "tAtsCd": "T001", "cnctrRate": "45" } ] },
                "totalCount": 1 } } }
        """.trimIndent()
    }
}
