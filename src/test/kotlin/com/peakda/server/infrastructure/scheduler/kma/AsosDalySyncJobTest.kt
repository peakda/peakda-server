package com.peakda.server.infrastructure.scheduler.kma

import com.peakda.server.domain.weather.application.WeatherDailyObservationSyncService
import com.peakda.server.domain.weather.repository.WeatherDailyObservationRepository
import com.peakda.server.infrastructure.external.kma.asosdaly.AsosDalyClient
import com.peakda.server.infrastructure.external.kma.asosdaly.AsosStationCatalog
import com.peakda.server.infrastructure.external.kma.asosdaly.response.AsosDalyItem
import com.peakda.server.infrastructure.scheduler.SchedulerProperties
import com.peakda.server.infrastructure.scheduler.SchedulerTime.KST
import com.peakda.server.infrastructure.scheduler.JobLogger
import com.peakda.server.infrastructure.scheduler.NoOpSchedulerJobLock
import com.peakda.server.infrastructure.scheduler.SchedulerJobSuccessGauge
import com.peakda.server.infrastructure.scheduler.kmaFixture
import com.peakda.server.infrastructure.scheduler.testErrorDecoder
import com.peakda.server.infrastructure.scheduler.testJobLogger
import com.peakda.server.infrastructure.scheduler.testObjectMapper
import com.peakda.server.infrastructure.scheduler.testResilience
import com.peakda.server.infrastructure.scheduler.history.SchedulerJobRunRecorder
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
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
    fun `한 지점 조회 실패 후에도 나머지 지점을 처리하고 잡은 실패로 기록한다`() {
        val yesterday = LocalDate.now(KST).minusDays(1)
        fixture.server.expect(requestTo(startsWith("https://example.test/asos/getWthrDataList?")))
            .andExpect(queryParam("stnIds", "108"))
            .andRespond(withServerError())
        fixture.server.expect(requestTo(startsWith("https://example.test/asos/getWthrDataList?")))
            .andExpect(queryParam("stnIds", "112"))
            .andRespond(withSuccess(successJson("112"), MediaType.APPLICATION_JSON))

        val recorder = RecordingRecorder()
        val registry = SimpleMeterRegistry()
        val jobLogger = JobLogger(recorder, registry, NoOpSchedulerJobLock, SchedulerJobSuccessGauge(registry))
        val job = AsosDalySyncJob(
            fixture.client,
            syncService,
            catalog,
            enabled(jobEnabled = true, backfillFrom = yesterday),
            jobLogger,
        )

        job.run()

        fixture.server.verify()
        assertThat(syncService.pages.flatten()).extracting<String> { it.stnId }.containsExactly("112")
        assertThat(recorder.events).hasSize(2)
        assertThat(recorder.events[0]).isEqualTo("start:${AsosDalySyncJob.JOB_NAME}")
        assertThat(recorder.events[1]).startsWith("fail:1:IllegalStateException:")
        assertThat(recorder.events[1]).contains("108")
        assertThat(registry.counter("scheduler.job.failure_total", "job", AsosDalySyncJob.JOB_NAME).count())
            .isEqualTo(1.0)
        assertThat(registry.counter("scheduler.job.success_total", "job", AsosDalySyncJob.JOB_NAME).count())
            .isZero()
    }

    @Test
    fun `모든 지점 조회가 실패해도 각 지점을 시도하고 실패로 기록한다`() {
        val yesterday = LocalDate.now(KST).minusDays(1)
        for (stationId in listOf("108", "112")) {
            fixture.server.expect(requestTo(startsWith("https://example.test/asos/getWthrDataList?")))
                .andExpect(queryParam("stnIds", stationId))
                .andRespond(withServerError())
        }

        val recorder = RecordingRecorder()
        val registry = SimpleMeterRegistry()
        val jobLogger = JobLogger(recorder, registry, NoOpSchedulerJobLock, SchedulerJobSuccessGauge(registry))
        val job = AsosDalySyncJob(
            fixture.client,
            syncService,
            catalog,
            enabled(jobEnabled = true, backfillFrom = yesterday),
            jobLogger,
        )

        job.run()

        fixture.server.verify()
        assertThat(syncService.pages).isEmpty()
        assertThat(recorder.events).hasSize(2)
        assertThat(recorder.events[0]).isEqualTo("start:${AsosDalySyncJob.JOB_NAME}")
        assertThat(recorder.events[1]).startsWith("fail:1:IllegalStateException:")
        assertThat(recorder.events[1]).contains("108", "112")
        assertThat(registry.counter("scheduler.job.failure_total", "job", AsosDalySyncJob.JOB_NAME).count())
            .isEqualTo(1.0)
        assertThat(registry.counter("scheduler.job.success_total", "job", AsosDalySyncJob.JOB_NAME).count())
            .isZero()
    }

    @Test
    fun `quota 초과는 이후 지점 처리를 중단하고 실행 이력을 skip으로 기록한다`() {
        val yesterday = LocalDate.now(KST).minusDays(1)
        fixture.server.expect(requestTo(startsWith("https://example.test/asos/getWthrDataList?")))
            .andExpect(queryParam("stnIds", "108"))
            .andRespond(withSuccess(quotaJson(), MediaType.APPLICATION_JSON))

        val recorder = RecordingRecorder()
        val registry = SimpleMeterRegistry()
        val jobLogger = JobLogger(recorder, registry, NoOpSchedulerJobLock, SchedulerJobSuccessGauge(registry))
        val job = AsosDalySyncJob(
            fixture.client,
            syncService,
            catalog,
            enabled(jobEnabled = true, backfillFrom = yesterday),
            jobLogger,
        )

        job.run()

        fixture.server.verify()
        assertThat(syncService.pages).isEmpty()
        assertThat(recorder.events).containsExactly(
            "start:${AsosDalySyncJob.JOB_NAME}",
            "skipExisting:1:quota_exhausted",
        )
        assertThat(registry.counter("scheduler.job.failure_total", "job", AsosDalySyncJob.JOB_NAME).count())
            .isZero()
        assertThat(
            registry.counter(
                "scheduler.job.skip_total",
                "job", AsosDalySyncJob.JOB_NAME,
                "reason", JobLogger.SKIP_QUOTA_EXHAUSTED,
            ).count(),
        ).isEqualTo(1.0)
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

    private class RecordingRecorder : SchedulerJobRunRecorder {
        val events = mutableListOf<String>()

        override fun start(jobName: String): Long? {
            events += "start:$jobName"
            return 1L
        }

        override fun complete(runId: Long?, processedCount: Int?, totalCount: Int?) {
            events += "complete:$runId:$processedCount:$totalCount"
        }

        override fun fail(runId: Long?, throwable: Throwable) {
            events += "fail:$runId:${throwable::class.simpleName}:${throwable.message}"
        }

        override fun skip(jobName: String, reason: String) {
            events += "skip:$jobName:$reason"
        }

        override fun skipExisting(runId: Long?, reason: String) {
            events += "skipExisting:$runId:$reason"
        }
    }

    companion object {
        private fun quotaJson() = """
            { "response": { "header": { "resultCode": "22", "resultMsg": "QUOTA_EXCEEDED" },
              "body": { "items": { "item": [] }, "totalCount": 0 } } }
        """.trimIndent()

        private fun successJson(stationId: String) = """
            { "response": { "header": { "resultCode": "00", "resultMsg": "NORMAL_SERVICE" },
              "body": { "items": { "item": [
                { "tm": "2026-07-29", "stnId": "$stationId", "stnNm": "테스트 지점",
                  "avgTa": "25.1", "minTa": "20.2", "maxTa": "30.4" }
              ] }, "totalCount": 1 } } }
        """.trimIndent()
    }
}
