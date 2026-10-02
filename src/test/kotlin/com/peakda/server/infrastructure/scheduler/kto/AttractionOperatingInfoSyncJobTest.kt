package com.peakda.server.infrastructure.scheduler.kto

import com.peakda.server.domain.attraction.application.AttractionEligibilityProperties
import com.peakda.server.domain.attraction.application.AttractionOperatingInfoSyncService
import com.peakda.server.domain.attraction.repository.AttractionOperatingInfoRepository
import com.peakda.server.domain.attraction.repository.AttractionOperatingInfoTarget
import com.peakda.server.infrastructure.external.kto.korservice.KorServiceClient
import com.peakda.server.infrastructure.external.kto.korservice.response.DetailInfoItem
import com.peakda.server.infrastructure.external.kto.korservice.response.DetailIntroItem
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

class AttractionOperatingInfoSyncJobTest {
    private val fixture = ktoFixture("https://example.test/kor", "KorService2") {
        KorServiceClient(it, testObjectMapper, testErrorDecoder, testResilience)
    }

    @Test
    fun `명소마다 소개정보와 반복정보를 받아 함께 저장한다`() {
        val syncService = RecordingSyncService(listOf(target("126128"), target("126129")))
        expect("detailIntro2", "126128", introJson("126128", usetime = "09:00~18:00"))
        expect("detailInfo2", "126128", infoJson("126128", "입 장 료", "무료"))
        expect("detailIntro2", "126129", introJson("126129", usetime = "상시 개방"))
        expect("detailInfo2", "126129", EMPTY_JSON)

        job(syncService, maxAttractions = 2).run()

        fixture.server.verify()
        assertThat(syncService.requestedLimit).isEqualTo(2)
        assertThat(syncService.saved.map { it.first.tourApiContentId }).containsExactly("126128", "126129")
        assertThat(syncService.saved[0].second?.usetime).isEqualTo("09:00~18:00")
        assertThat(syncService.saved[0].third.map { it.infotext }).containsExactly("무료")
        assertThat(syncService.saved[1].third).isEmpty()
    }

    @Test
    fun `요청 오류가 난 명소는 건너뛰고 다음 명소를 받는다`() {
        val syncService = RecordingSyncService(listOf(target("126128"), target("126129")))
        expect("detailIntro2", "126128", errorJson("10"))
        expect("detailIntro2", "126129", introJson("126129", usetime = "상시 개방"))
        expect("detailInfo2", "126129", EMPTY_JSON)

        job(syncService).run()

        fixture.server.verify()
        assertThat(syncService.saved.map { it.first.tourApiContentId }).containsExactly("126129")
    }

    @Test
    fun `호출 한도에 걸리면 남은 명소를 받지 않고 멈춘다`() {
        val syncService = RecordingSyncService(listOf(target("126128"), target("126129")))
        expect("detailIntro2", "126128", errorJson("22"))

        job(syncService).run()

        fixture.server.verify()
        assertThat(syncService.saved).isEmpty()
    }

    @Test
    fun `enabled=false 이면 대상을 고르지도 호출하지도 않는다`() {
        val syncService = RecordingSyncService(listOf(target("126128")))

        AttractionOperatingInfoSyncJob(fixture.client, syncService, props(jobEnabled = false), testJobLogger()).run()

        fixture.server.verify()
        assertThat(syncService.requestedLimit).isNull()
    }

    private fun job(syncService: AttractionOperatingInfoSyncService, maxAttractions: Int = 10) =
        AttractionOperatingInfoSyncJob(fixture.client, syncService, props(maxAttractions = maxAttractions), testJobLogger())

    private fun expect(operation: String, contentId: String, json: String) {
        fixture.server.expect(
            requestTo(startsWith("https://example.test/kor/$operation?contentId=$contentId&contentTypeId=12")),
        ).andRespond(withSuccess(json, MediaType.APPLICATION_JSON))
    }

    private fun props(jobEnabled: Boolean = true, maxAttractions: Int = 10) = SchedulerProperties(
        enabled = true,
        kto = SchedulerProperties.KtoSchedulerProps(
            operatingInfo = SchedulerProperties.OperatingInfoJobProps(
                cron = "* * * * * *",
                enabled = jobEnabled,
                maxAttractions = maxAttractions,
            ),
        ),
    )

    private fun target(contentId: String) = AttractionOperatingInfoTarget(
        attractionId = contentId.toLong(),
        tourApiContentId = contentId,
        contentTypeCode = "12",
        externalModifiedAt = "20260901120000",
    )

    private class RecordingSyncService(
        private val targets: List<AttractionOperatingInfoTarget>,
    ) : AttractionOperatingInfoSyncService(
        Mockito.mock(AttractionOperatingInfoRepository::class.java),
        AttractionEligibilityProperties(),
    ) {
        var requestedLimit: Int? = null
        val saved = mutableListOf<Triple<AttractionOperatingInfoTarget, DetailIntroItem?, List<DetailInfoItem>>>()

        override fun findTargets(limit: Int): List<AttractionOperatingInfoTarget> {
            requestedLimit = limit
            return targets.take(limit)
        }

        override fun save(target: AttractionOperatingInfoTarget, intro: DetailIntroItem?, infos: List<DetailInfoItem>): Int {
            saved += Triple(target, intro, infos)
            return 1
        }
    }

    companion object {
        private val EMPTY_JSON = """
            { "response": { "header": { "resultCode": "0000", "resultMsg": "OK" },
              "body": { "items": "", "numOfRows": 10, "pageNo": 1, "totalCount": 0 } } }
        """.trimIndent()

        private fun introJson(contentId: String, usetime: String) = """
            { "response": { "header": { "resultCode": "0000", "resultMsg": "OK" },
              "body": { "items": { "item": [ { "contentid": "$contentId", "contenttypeid": "12",
                "usetime": "$usetime", "restdate": "연중무휴", "parking": "가능" } ] },
                "numOfRows": 10, "pageNo": 1, "totalCount": 1 } } }
        """.trimIndent()

        private fun infoJson(contentId: String, infoname: String, infotext: String) = """
            { "response": { "header": { "resultCode": "0000", "resultMsg": "OK" },
              "body": { "items": { "item": [ { "contentid": "$contentId", "contenttypeid": "12",
                "fldgubun": "3", "infoname": "$infoname", "infotext": "$infotext", "serialnum": "0" } ] },
                "numOfRows": 10, "pageNo": 1, "totalCount": 1 } } }
        """.trimIndent()

        private fun errorJson(resultCode: String) = """
            { "response": { "header": { "resultCode": "$resultCode", "resultMsg": "ERROR" },
              "body": { "items": { "item": [] }, "totalCount": 0 } } }
        """.trimIndent()
    }
}
