package com.peakda.server.infrastructure.scheduler.kto

import com.peakda.server.domain.congestion.application.CongestionSyncService
import com.peakda.server.domain.congestion.repository.CongestionRepository
import com.peakda.server.infrastructure.external.kto.tatscnctr.TatsCnctrClient
import com.peakda.server.infrastructure.external.kto.tatscnctr.TatsCnctrRegionCatalog
import com.peakda.server.infrastructure.external.kto.tatscnctr.response.CnctrRateItem
import com.peakda.server.infrastructure.scheduler.InMemorySchedulerJobCursor
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
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess

class TatsCnctrSyncJobTest {
    private val fixture = ktoFixture("https://example.test/tats", "TatsCnctrRateService") {
        TatsCnctrClient(it, testObjectMapper, testErrorDecoder, testResilience)
    }
    private val syncService = RecordingCongestionSync()
    private val cursor = InMemorySchedulerJobCursor()

    @Test
    fun `run 시 시군구마다 tatsCnctrRatedList를 호출해 sync service에 페이지를 전달한다`() {
        fixture.server.expect(requestTo(uriFor("11", "11110")))
            .andRespond(withSuccess(successJson("종로구 관광지"), MediaType.APPLICATION_JSON))
        fixture.server.expect(requestTo(uriFor("26", "26110")))
            .andRespond(withSuccess(successJson("중구 관광지"), MediaType.APPLICATION_JSON))

        val job = TatsCnctrSyncJob(fixture.client, twoRegionCatalog(), syncService, enabled(true), testJobLogger(), cursor)
        job.run()

        fixture.server.verify()
        assertThat(syncService.pages.flatten())
            .extracting<String> { it.tAtsNm }
            .containsExactly("종로구 관광지", "중구 관광지")
    }

    @Test
    fun `enabled=false 이면 client와 sync service 모두 호출하지 않는다`() {
        val job = TatsCnctrSyncJob(fixture.client, twoRegionCatalog(), syncService, enabled(false), testJobLogger(), cursor)

        job.run()

        fixture.server.verify()
        assertThat(syncService.pages).isEmpty()
    }

    @Test
    fun `이전 실행이 멈춘 시군구부터 원형으로 이어서 순회하고 커서를 한 바퀴 뒤 위치로 남긴다`() {
        cursor.save(TatsCnctrSyncJob.JOB_NAME, 1)
        fixture.server.expect(requestTo(uriFor("26", "26110")))
            .andRespond(withSuccess(successJson("중구 관광지"), MediaType.APPLICATION_JSON))
        fixture.server.expect(requestTo(uriFor("11", "11110")))
            .andRespond(withSuccess(successJson("종로구 관광지"), MediaType.APPLICATION_JSON))

        val job = TatsCnctrSyncJob(fixture.client, twoRegionCatalog(), syncService, enabled(true), testJobLogger(), cursor)
        job.run()

        fixture.server.verify()
        assertThat(syncService.pages.flatten()).extracting<String> { it.tAtsNm }
            .containsExactly("중구 관광지", "종로구 관광지")
        assertThat(cursor.load(TatsCnctrSyncJob.JOB_NAME)).isEqualTo(1)
    }

    @Test
    fun `호출 한도(429)에 걸리면 즉시 멈추고 다음 실행이 실패한 시군구부터 시작하도록 커서를 남긴다`() {
        fixture.server.expect(requestTo(uriFor("11", "11110")))
            .andRespond(withSuccess(successJson("종로구 관광지"), MediaType.APPLICATION_JSON))
        fixture.server.expect(requestTo(uriFor("26", "26110")))
            .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS))

        val job = TatsCnctrSyncJob(fixture.client, twoRegionCatalog(), syncService, enabled(true), testJobLogger(), cursor)
        job.run()

        fixture.server.verify()
        assertThat(cursor.load(TatsCnctrSyncJob.JOB_NAME)).isEqualTo(1)
    }

    @Test
    fun `한도 초과가 아닌 오류로 실패한 시군구는 건너뛰고 나머지를 계속 수집한다`() {
        fixture.server.expect(requestTo(uriFor("11", "11110")))
            .andRespond(withSuccess(ERROR_JSON, MediaType.APPLICATION_JSON))
        fixture.server.expect(requestTo(uriFor("26", "26110")))
            .andRespond(withSuccess(successJson("중구 관광지"), MediaType.APPLICATION_JSON))

        val job = TatsCnctrSyncJob(fixture.client, twoRegionCatalog(), syncService, enabled(true), testJobLogger(), cursor)
        job.run()

        fixture.server.verify()
        assertThat(syncService.pages.flatten()).extracting<String> { it.tAtsNm }.containsExactly("중구 관광지")
        assertThat(cursor.load(TatsCnctrSyncJob.JOB_NAME)).isEqualTo(0)
    }

    @Test
    fun `커서가 시군구 목록 범위를 벗어나면 처음부터 순회한다`() {
        cursor.save(TatsCnctrSyncJob.JOB_NAME, 99)
        fixture.server.expect(requestTo(uriFor("11", "11110")))
            .andRespond(withSuccess(successJson("종로구 관광지"), MediaType.APPLICATION_JSON))
        fixture.server.expect(requestTo(uriFor("26", "26110")))
            .andRespond(withSuccess(successJson("중구 관광지"), MediaType.APPLICATION_JSON))

        val job = TatsCnctrSyncJob(fixture.client, twoRegionCatalog(), syncService, enabled(true), testJobLogger(), cursor)
        job.run()

        fixture.server.verify()
        assertThat(cursor.load(TatsCnctrSyncJob.JOB_NAME)).isEqualTo(0)
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
                "?numOfRows=1000&pageNo=1&areaCd=$areaCd&signguCd=$signguCd" +
                "&serviceKey=test-key&MobileOS=ETC&MobileApp=peakda-test&_type=json"

        private val ERROR_JSON = """
            { "response": { "header": { "resultCode": "10", "resultMsg": "INVALID_REQUEST_PARAMETER_ERROR" },
              "body": { "items": "", "totalCount": 0 } } }
        """.trimIndent()

        private fun successJson(attractionName: String) = """
            { "response": { "header": { "resultCode": "0000", "resultMsg": "OK" },
              "body": { "items": { "item": [ { "baseYmd": "20260512", "tAtsNm": "$attractionName", "cnctrRate": "45" } ] },
                "totalCount": 1 } } }
        """.trimIndent()
    }
}
