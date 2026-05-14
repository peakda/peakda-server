package com.peakda.server.infrastructure.scheduler.kto

import com.peakda.server.domain.visitor.application.RegionVisitorSyncService
import com.peakda.server.domain.visitor.repository.RegionVisitorRepository
import com.peakda.server.infrastructure.external.kto.datalab.DataLabClient
import com.peakda.server.infrastructure.external.kto.datalab.response.MetcoVisitrItem
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

class DataLabSyncJobTest {
    private val fixture = ktoFixture("https://example.test/datalab", "DataLabService") {
        DataLabClient(it, testObjectMapper, testErrorDecoder)
    }
    private val syncService = RecordingVisitorSync()

    @Test
    fun `run 시 metcoRegnVisitrDDList를 호출해 sync service에 페이지를 전달한다`() {
        fixture.server.expect(
            requestTo(startsWith("https://example.test/datalab/metcoRegnVisitrDDList?numOfRows=100&pageNo=1&startYmd=")),
        ).andRespond(withSuccess(SUCCESS_JSON, MediaType.APPLICATION_JSON))

        val job = DataLabSyncJob(fixture.client, syncService, enabled(true), testJobLogger())
        job.run()

        fixture.server.verify()
        assertThat(syncService.pages.flatten()).extracting<String> { it.areaCd }.containsExactly("11")
    }

    @Test
    fun `enabled=false 이면 client와 sync service 모두 호출하지 않는다`() {
        val job = DataLabSyncJob(fixture.client, syncService, enabled(false), testJobLogger())

        job.run()

        fixture.server.verify()
        assertThat(syncService.pages).isEmpty()
    }

    private fun enabled(jobEnabled: Boolean) = SchedulerProperties(
        enabled = true,
        kto = SchedulerProperties.KtoSchedulerProps(
            dataLab = SchedulerProperties.JobProps(cron = "* * * * * *", enabled = jobEnabled),
        ),
    )

    private class RecordingVisitorSync :
        RegionVisitorSyncService(Mockito.mock(RegionVisitorRepository::class.java)) {
        val pages = mutableListOf<List<MetcoVisitrItem>>()
        override fun upsertPage(items: List<MetcoVisitrItem>): Int {
            pages += items.toList(); return items.size
        }
    }

    companion object {
        private val SUCCESS_JSON = """
            { "response": { "header": { "resultCode": "0000", "resultMsg": "OK" },
              "body": { "items": { "item": [
                { "baseYmd": "20260512", "areaCd": "11", "touDivCd": "1", "num": "100" }
              ] }, "totalCount": 1 } } }
        """.trimIndent()
    }
}
