package com.peakda.server.infrastructure.scheduler.kma

import com.peakda.server.domain.weather.application.WeatherMidForecastSyncService
import com.peakda.server.domain.weather.repository.WeatherMidForecastRepository
import com.peakda.server.infrastructure.external.kma.midfcst.MidFcstClient
import com.peakda.server.infrastructure.external.kma.midfcst.MidRegionCode
import com.peakda.server.infrastructure.external.kma.midfcst.response.MidLandFcstItem
import com.peakda.server.infrastructure.external.kma.midfcst.response.MidTaItem
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
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess

class MidFcstSyncJobTest {
    private val fixture = kmaFixture("https://example.test/mid", "MidFcstInfoService") {
        MidFcstClient(it, testObjectMapper, testErrorDecoder)
    }
    private val syncService = RecordingMidSync()

    @Test
    fun `run 시 모든 region에 대해 land와 ta 를 호출한다`() {
        val regions = MidRegionCode.entries.size
        fixture.server.expect(
            ExpectedCount.times(regions),
            requestTo(startsWith("https://example.test/mid/getMidLandFcst")),
        ).andRespond(withSuccess(LAND_JSON, MediaType.APPLICATION_JSON))
        fixture.server.expect(
            ExpectedCount.times(regions),
            requestTo(startsWith("https://example.test/mid/getMidTa")),
        ).andRespond(withSuccess(TA_JSON, MediaType.APPLICATION_JSON))

        val job = MidFcstSyncJob(fixture.client, syncService, enabled(true), testJobLogger())
        job.run()

        fixture.server.verify()
        assertThat(syncService.landCalls).isEqualTo(regions)
        assertThat(syncService.taCalls).isEqualTo(regions)
        assertThat(syncService.landRegions).contains("SEOUL" to "11B00000")
        assertThat(syncService.taRegions).contains("SEOUL" to "11B10101")
    }

    @Test
    fun `enabled=false 이면 호출하지 않는다`() {
        val job = MidFcstSyncJob(fixture.client, syncService, enabled(false), testJobLogger())

        job.run()

        fixture.server.verify()
        assertThat(syncService.landCalls).isZero
    }

    private fun enabled(jobEnabled: Boolean) = SchedulerProperties(
        enabled = true,
        kma = SchedulerProperties.KmaSchedulerProps(
            midFcst = SchedulerProperties.JobProps(cron = "* * * * * *", enabled = jobEnabled),
        ),
    )

    private class RecordingMidSync :
        WeatherMidForecastSyncService(Mockito.mock(WeatherMidForecastRepository::class.java)) {
        var landCalls = 0
        var taCalls = 0
        val landRegions = mutableListOf<Pair<String, String>>()
        val taRegions = mutableListOf<Pair<String, String>>()
        override fun upsertLand(
            regionCode: String,
            sourceRegionCode: String,
            announceTime: String,
            item: MidLandFcstItem,
        ): Int {
            landRegions += regionCode to sourceRegionCode
            landCalls++; return 1
        }
        override fun upsertTa(
            regionCode: String,
            sourceRegionCode: String,
            announceTime: String,
            item: MidTaItem,
        ): Int {
            taRegions += regionCode to sourceRegionCode
            taCalls++; return 1
        }
    }

    companion object {
        private val LAND_JSON = """
            { "response": { "header": { "resultCode": "00", "resultMsg": "NORMAL_SERVICE" },
              "body": { "items": { "item": [ { "regId": "11B00000", "wf3Am": "맑음" } ] }, "totalCount": 1 } } }
        """.trimIndent()
        private val TA_JSON = """
            { "response": { "header": { "resultCode": "00", "resultMsg": "NORMAL_SERVICE" },
              "body": { "items": { "item": [ { "regId": "11B10101", "taMin3": 10, "taMax3": 20 } ] }, "totalCount": 1 } } }
        """.trimIndent()
    }
}
