package com.peakda.server.infrastructure.scheduler.kma

import com.peakda.server.domain.weather.application.WeatherShortForecastSyncService
import com.peakda.server.domain.weather.repository.WeatherShortForecastRepository
import com.peakda.server.infrastructure.external.kma.vilagefcst.VilageFcstClient
import com.peakda.server.infrastructure.external.kma.vilagefcst.response.VilageFcstItem
import com.peakda.server.infrastructure.scheduler.SchedulerProperties
import com.peakda.server.infrastructure.scheduler.kmaFixture
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

class VilageFcstSyncJobTest {
    private val fixture = kmaFixture("https://example.test/vilage", "VilageFcstInfoService") {
        VilageFcstClient(it, testObjectMapper, testErrorDecoder)
    }
    private val syncService = RecordingShortFcstSync()

    @Test
    fun `run 시 grid 별로 getVilageFcst를 호출해 sync service에 페이지를 전달한다`() {
        fixture.server.expect(
            requestTo(startsWith("https://example.test/vilage/getVilageFcst?numOfRows=1000&pageNo=1&base_date=")),
        ).andRespond(withSuccess(SUCCESS_JSON, MediaType.APPLICATION_JSON))

        val job = VilageFcstSyncJob(fixture.client, syncService, enabled(true), testJobLogger())
        job.run()

        fixture.server.verify()
        assertThat(syncService.pages.flatten()).extracting<String> { it.category }.containsExactly("T1H")
    }

    @Test
    fun `enabled=false 이면 client와 sync service 모두 호출하지 않는다`() {
        val job = VilageFcstSyncJob(fixture.client, syncService, enabled(false), testJobLogger())

        job.run()

        fixture.server.verify()
        assertThat(syncService.pages).isEmpty()
    }

    private fun enabled(jobEnabled: Boolean) = SchedulerProperties(
        enabled = true,
        kma = SchedulerProperties.KmaSchedulerProps(
            vilageFcst = SchedulerProperties.VilageFcstJobProps(
                cron = "* * * * * *",
                enabled = jobEnabled,
                grids = listOf(SchedulerProperties.VilageFcstJobProps.Grid("서울", 60, 127)),
            ),
        ),
    )

    private class RecordingShortFcstSync :
        WeatherShortForecastSyncService(Mockito.mock(WeatherShortForecastRepository::class.java)) {
        val pages = mutableListOf<List<VilageFcstItem>>()
        override fun upsertPage(items: List<VilageFcstItem>): Int {
            pages += items.toList(); return items.size
        }
    }

    companion object {
        private val SUCCESS_JSON = """
            { "response": { "header": { "resultCode": "00", "resultMsg": "NORMAL_SERVICE" },
              "body": { "items": { "item": [
                { "baseDate": "20260512", "baseTime": "0500", "category": "T1H",
                  "fcstDate": "20260512", "fcstTime": "0600", "fcstValue": "10", "nx": 60, "ny": 127 }
              ] }, "totalCount": 1 } } }
        """.trimIndent()
    }
}
