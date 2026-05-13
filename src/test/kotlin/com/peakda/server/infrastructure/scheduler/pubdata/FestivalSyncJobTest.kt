package com.peakda.server.infrastructure.scheduler.pubdata

import com.peakda.server.domain.festival.application.FestivalSyncService
import com.peakda.server.domain.festival.repository.FestivalRepository
import com.peakda.server.infrastructure.external.pubdata.festival.FestivalClient
import com.peakda.server.infrastructure.external.pubdata.festival.response.FestivalItem
import com.peakda.server.infrastructure.scheduler.SchedulerProperties
import com.peakda.server.infrastructure.scheduler.pubdataFixture
import com.peakda.server.infrastructure.scheduler.testErrorDecoder
import com.peakda.server.infrastructure.scheduler.testJobLogger
import com.peakda.server.infrastructure.scheduler.testObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess

class FestivalSyncJobTest {
    private val fixture = pubdataFixture("https://example.test/festival", "PublicCultureFestival") {
        FestivalClient(it, testObjectMapper, testErrorDecoder)
    }
    private val syncService = RecordingFestivalSync()

    @Test
    fun `run 시 list를 호출해 sync service에 페이지를 전달한다`() {
        fixture.server.expect(
            requestTo("https://example.test/festival?numOfRows=100&pageNo=1&serviceKey=test-key&_type=json"),
        ).andRespond(withSuccess(SUCCESS_JSON, MediaType.APPLICATION_JSON))

        val job = FestivalSyncJob(fixture.client, syncService, enabled(true), testJobLogger())
        job.run()

        fixture.server.verify()
        assertThat(syncService.pages.flatten()).extracting<String> { it.fstvlNm }.containsExactly("테스트축제")
    }

    @Test
    fun `enabled=false 이면 client와 sync service 모두 호출하지 않는다`() {
        val job = FestivalSyncJob(fixture.client, syncService, enabled(false), testJobLogger())

        job.run()

        fixture.server.verify()
        assertThat(syncService.pages).isEmpty()
    }

    private fun enabled(jobEnabled: Boolean) = SchedulerProperties(
        enabled = true,
        pubdata = SchedulerProperties.PubdataSchedulerProps(
            festival = SchedulerProperties.JobProps(cron = "* * * * * *", enabled = jobEnabled),
        ),
    )

    private class RecordingFestivalSync :
        FestivalSyncService(Mockito.mock(FestivalRepository::class.java)) {
        val pages = mutableListOf<List<FestivalItem>>()
        override fun upsertPage(items: List<FestivalItem>): Int {
            pages += items.toList(); return items.size
        }
    }

    companion object {
        private val SUCCESS_JSON = """
            { "response": { "header": { "resultCode": "00", "resultMsg": "NORMAL_SERVICE" },
              "body": { "items": { "item": [
                { "fstvlNm": "테스트축제", "opar": "서울", "fstvlStartDate": "20260501" }
              ] }, "totalCount": 1 } } }
        """.trimIndent()
    }
}
