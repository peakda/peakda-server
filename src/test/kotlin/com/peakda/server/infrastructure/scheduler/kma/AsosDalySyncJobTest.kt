package com.peakda.server.infrastructure.scheduler.kma

import com.peakda.server.domain.weather.application.WeatherDailyObservationSyncService
import com.peakda.server.domain.weather.repository.WeatherDailyObservationRepository
import com.peakda.server.infrastructure.external.kma.asosdaly.AsosDalyClient
import com.peakda.server.infrastructure.external.kma.asosdaly.AsosStationCatalog
import com.peakda.server.infrastructure.external.kma.asosdaly.response.AsosDalyItem
import com.peakda.server.infrastructure.scheduler.SchedulerProperties
import com.peakda.server.infrastructure.scheduler.SchedulerTime.KST
import com.peakda.server.infrastructure.scheduler.kmaFixture
import com.peakda.server.infrastructure.scheduler.testErrorDecoder
import com.peakda.server.infrastructure.scheduler.testJobLogger
import com.peakda.server.infrastructure.scheduler.testObjectMapper
import com.peakda.server.infrastructure.scheduler.testResilience
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import java.time.LocalDate

class AsosDalySyncJobTest {
    private val fixture = kmaFixture("https://example.test/asos", "AsosDalyInfoService") {
        AsosDalyClient(it, testObjectMapper, testErrorDecoder, testResilience)
    }
    private val syncService = RecordingDailyObservationSync()
    private val catalog = AsosStationCatalog(
        ByteArrayResource(
            """
                stnId,name,latitude,longitude,altitude
                108,서울,37.57142,126.9658,85.67
                112,인천,37.47772,126.6249,68.99
            """.trimIndent().toByteArray(),
        ),
    )

    @Test
    fun `run 시 지점별로 getWthrDataList를 호출해 sync service에 페이지를 전달한다`() {
        val yesterday = LocalDate.now(KST).minusDays(1)
        for (stationId in listOf("108", "112")) {
            fixture.server.expect(requestTo(startsWith("https://example.test/asos/getWthrDataList?")))
                .andExpect(queryParam("dataCd", "ASOS"))
                .andExpect(queryParam("dateCd", "DAY"))
                .andExpect(queryParam("stnIds", stationId))
                .andRespond(withSuccess(successJson(stationId), MediaType.APPLICATION_JSON))
        }
        val job = AsosDalySyncJob(
            fixture.client,
            syncService,
            catalog,
            enabled(jobEnabled = true, backfillFrom = yesterday),
            testJobLogger(),
        )

        job.run()

        fixture.server.verify()
        assertThat(syncService.pages.flatten()).extracting<String> { it.stnId }.containsExactly("108", "112")
    }

    @Test
    fun `설정 지점이 비어 있으면 카탈로그 전체 지점을 사용한다`() {
        val yesterday = LocalDate.now(KST).minusDays(1)
        for (stationId in listOf("108", "112")) {
            fixture.server.expect(requestTo(startsWith("https://example.test/asos/getWthrDataList?")))
                .andExpect(queryParam("stnIds", stationId))
                .andRespond(withSuccess(successJson(stationId), MediaType.APPLICATION_JSON))
        }
        val job = AsosDalySyncJob(
            fixture.client,
            syncService,
            catalog,
            enabled(jobEnabled = true, backfillFrom = yesterday, stations = emptyList()),
            testJobLogger(),
        )

        job.run()

        fixture.server.verify()
        assertThat(syncService.pages.flatten()).extracting<String> { it.stnId }.containsExactly("108", "112")
    }

    @Test
    fun `enabled=false 이면 client와 sync service 모두 호출하지 않는다`() {
        val job = AsosDalySyncJob(fixture.client, syncService, catalog, enabled(false), testJobLogger())

        job.run()

        fixture.server.verify()
        assertThat(syncService.latestLookupCount).isZero()
        assertThat(syncService.pages).isEmpty()
    }

    @Test
    fun `관측 이력이 없으면 backfillFrom부터 시작한다`() {
        val range = AsosDalySyncJob.resolveBackfillRange(
            lastObserved = null,
            backfillFrom = LocalDate.of(2026, 1, 1),
            yesterday = LocalDate.of(2026, 1, 10),
            maxBackfillDays = 400,
        )

        assertThat(range).isEqualTo(LocalDate.of(2026, 1, 1)..LocalDate.of(2026, 1, 10))
    }

    @Test
    fun `마지막 관측일 다음날부터 시작한다`() {
        val range = AsosDalySyncJob.resolveBackfillRange(
            lastObserved = LocalDate.of(2026, 1, 5),
            backfillFrom = LocalDate.of(2026, 1, 1),
            yesterday = LocalDate.of(2026, 1, 10),
            maxBackfillDays = 400,
        )

        assertThat(range).isEqualTo(LocalDate.of(2026, 1, 6)..LocalDate.of(2026, 1, 10))
    }

    @Test
    fun `이미 어제까지 채워져 있으면 조회 구간이 없다`() {
        val range = AsosDalySyncJob.resolveBackfillRange(
            lastObserved = LocalDate.of(2026, 1, 10),
            backfillFrom = LocalDate.of(2026, 1, 1),
            yesterday = LocalDate.of(2026, 1, 10),
            maxBackfillDays = 400,
        )

        assertThat(range).isNull()
    }

    @Test
    fun `백필 구간이 maxBackfillDays를 넘으면 상한에서 잘린다`() {
        val range = AsosDalySyncJob.resolveBackfillRange(
            lastObserved = null,
            backfillFrom = LocalDate.of(2026, 1, 1),
            yesterday = LocalDate.of(2026, 12, 31),
            maxBackfillDays = 30,
        )

        assertThat(range).isEqualTo(LocalDate.of(2026, 1, 1)..LocalDate.of(2026, 1, 30))
    }

    private fun enabled(
        jobEnabled: Boolean,
        backfillFrom: LocalDate = LocalDate.of(2026, 1, 1),
        stations: List<String> = listOf("108", "112"),
    ) = SchedulerProperties(
        enabled = true,
        kma = SchedulerProperties.KmaSchedulerProps(
            asosDaly = SchedulerProperties.AsosDalyJobProps(
                cron = "* * * * * *",
                enabled = jobEnabled,
                backfillFrom = backfillFrom,
                maxBackfillDays = 400,
                stations = stations,
            ),
        ),
    )

    private class RecordingDailyObservationSync :
        WeatherDailyObservationSyncService(Mockito.mock(WeatherDailyObservationRepository::class.java)) {
        val pages = mutableListOf<List<AsosDalyItem>>()
        var latestLookupCount = 0

        override fun upsertPage(items: List<AsosDalyItem>): Int {
            pages += items.toList()
            return items.size
        }

        override fun findLatestObservedOnByStation(): Map<String, LocalDate> {
            latestLookupCount++
            return emptyMap()
        }
    }

    companion object {
        private fun successJson(stationId: String) = """
            { "response": { "header": { "resultCode": "00", "resultMsg": "NORMAL_SERVICE" },
              "body": { "items": { "item": [
                { "tm": "2026-07-29", "stnId": "$stationId", "stnNm": "테스트 지점",
                  "avgTa": "25.1", "minTa": "20.2", "maxTa": "30.4" }
              ] }, "totalCount": 1 } } }
        """.trimIndent()
    }
}
