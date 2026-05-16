package com.peakda.server.infrastructure.scheduler.kto

import com.peakda.server.domain.attraction.application.AttractionSyncService
import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.infrastructure.external.kto.korservice.KorServiceClient
import com.peakda.server.infrastructure.external.kto.korservice.response.AreaBasedSyncListItem
import com.peakda.server.infrastructure.scheduler.SchedulerProperties
import com.peakda.server.infrastructure.scheduler.ktoFixture
import com.peakda.server.infrastructure.scheduler.testErrorDecoder
import com.peakda.server.infrastructure.scheduler.testJobLogger
import com.peakda.server.infrastructure.scheduler.testObjectMapper
import com.peakda.server.infrastructure.scheduler.testResilience
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess

class KorServiceSyncJobTest {
    private val fixture = ktoFixture("https://example.test/kor", "KorService2") {
        KorServiceClient(it, testObjectMapper, testErrorDecoder, testResilience)
    }
    private val syncService = RecordingAttractionSyncService()

    @Test
    fun `run 시 areaBasedSyncList2를 호출해 sync service에 페이지를 전달한다`() {
        fixture.server.expect(
            requestTo(startsWith("https://example.test/kor/areaBasedSyncList2?numOfRows=100&pageNo=1&modifiedtime=")),
        ).andRespond(withSuccess(SUCCESS_JSON, MediaType.APPLICATION_JSON))

        val job = KorServiceSyncJob(fixture.client, syncService, enabled(true), testJobLogger())
        job.run()

        fixture.server.verify()
        assertThat(syncService.pages).hasSize(1)
        assertThat(syncService.pages[0]).hasSize(1)
        assertThat(syncService.pages[0][0].contentid).isEqualTo("126128")
    }

    @Test
    fun `enabled=false 이면 client와 sync service 모두 호출하지 않는다`() {
        val job = KorServiceSyncJob(fixture.client, syncService, enabled(false), testJobLogger())

        job.run()

        fixture.server.verify()
        assertThat(syncService.pages).isEmpty()
    }

    private fun enabled(jobEnabled: Boolean) = SchedulerProperties(
        enabled = true,
        kto = SchedulerProperties.KtoSchedulerProps(
            korService = SchedulerProperties.JobProps(cron = "* * * * * *", enabled = jobEnabled),
        ),
    )

    private class RecordingAttractionSyncService :
        AttractionSyncService(Mockito.mock(AttractionRepository::class.java)) {
        val pages = mutableListOf<List<AreaBasedSyncListItem>>()
        override fun upsertPage(items: List<AreaBasedSyncListItem>): Int {
            pages += items.toList()
            return items.size
        }
    }

    companion object {
        private val SUCCESS_JSON = """
            { "response": { "header": { "resultCode": "0000", "resultMsg": "OK" },
              "body": { "items": { "item": [ { "contentid": "126128", "title": "경복궁", "showflag": "1" } ] },
                "numOfRows": 100, "pageNo": 1, "totalCount": 1 } } }
        """.trimIndent()
    }
}
