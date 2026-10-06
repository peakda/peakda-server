package com.peakda.server.infrastructure.scheduler.kto

import com.peakda.server.domain.attraction.application.AttractionEligibilityProperties
import com.peakda.server.domain.attraction.application.AttractionThumbnailCheckService
import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.attraction.repository.AttractionThumbnailCheckTarget
import com.peakda.server.infrastructure.external.kto.image.KtoImageProbeClient
import com.peakda.server.infrastructure.scheduler.SchedulerProperties
import com.peakda.server.infrastructure.scheduler.testJobLogger
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.HttpStatus
import org.springframework.test.web.client.ExpectedCount.never
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.anything
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.web.client.RestClient
import java.time.Duration

class AttractionThumbnailCheckJobTest {
    private val builder = RestClient.builder()
    private val server = MockRestServiceServer.bindTo(builder).build()
    private val probeClient = KtoImageProbeClient(builder.build())

    @Test
    fun `있으면 정상, 없으면 깨짐으로 저장하고 판단할 수 없는 명소는 저장하지 않는다`() {
        val checkService = RecordingCheckService(listOf(target(1), target(2), target(3)))
        server.expect(requestTo(url(1))).andRespond(withStatus(HttpStatus.PARTIAL_CONTENT))
        server.expect(requestTo(url(2))).andRespond(withStatus(HttpStatus.NOT_FOUND))
        server.expect(requestTo(url(3))).andRespond(withStatus(HttpStatus.BAD_GATEWAY))

        job(checkService, maxAttractions = 3).run()

        server.verify()
        assertThat(checkService.requested).isEqualTo(3 to Duration.ofDays(7))
        assertThat(checkService.saved).containsExactly(target(1) to false, target(2) to true)
    }

    @Test
    fun `관광공사 이미지 서버 주소가 아니면 요청하지 않고 확인한 것으로 저장한다`() {
        val other = AttractionThumbnailCheckTarget(9, "https://example.com/image.jpg")
        val checkService = RecordingCheckService(listOf(other))
        server.expect(never(), anything())

        job(checkService).run()

        server.verify()
        assertThat(checkService.saved).containsExactly(other to false)
    }

    @Test
    fun `판단할 수 없는 응답이 연달아 이어지면 남은 명소를 확인하지 않고 멈춘다`() {
        val limit = AttractionThumbnailCheckJob.MAX_CONSECUTIVE_UNKNOWN
        val targets = (1L..limit + 1L).map(::target)
        val checkService = RecordingCheckService(targets)
        targets.take(limit).forEach { server.expect(requestTo(it.thumbnailImageUrl)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)) }

        job(checkService, maxAttractions = targets.size).run()

        server.verify()
        assertThat(checkService.saved).isEmpty()
    }

    @Test
    fun `enabled=false 이면 대상을 고르지도 요청하지도 않는다`() {
        val checkService = RecordingCheckService(listOf(target(1)))

        job(checkService, jobEnabled = false).run()

        server.verify()
        assertThat(checkService.requested).isNull()
    }

    private fun job(checkService: AttractionThumbnailCheckService, jobEnabled: Boolean = true, maxAttractions: Int = 10) =
        AttractionThumbnailCheckJob(probeClient, checkService, props(jobEnabled, maxAttractions), testJobLogger())

    private fun props(jobEnabled: Boolean, maxAttractions: Int) = SchedulerProperties(
        enabled = true,
        kto = SchedulerProperties.KtoSchedulerProps(
            thumbnailCheck = SchedulerProperties.ThumbnailCheckJobProps(
                cron = "* * * * * *",
                enabled = jobEnabled,
                maxAttractions = maxAttractions,
                recheckAfter = Duration.ofDays(7),
            ),
        ),
    )

    private fun target(id: Long) = AttractionThumbnailCheckTarget(id, url(id))

    private fun url(id: Long) = "http://tong.visitkorea.or.kr/cms/resource/30/${id}_image3_1.jpg"

    private class RecordingCheckService(
        private val targets: List<AttractionThumbnailCheckTarget>,
    ) : AttractionThumbnailCheckService(
        Mockito.mock(AttractionRepository::class.java),
        AttractionEligibilityProperties(),
    ) {
        var requested: Pair<Int, Duration>? = null
        val saved = mutableListOf<Pair<AttractionThumbnailCheckTarget, Boolean>>()

        override fun findTargets(limit: Int, recheckAfter: Duration): List<AttractionThumbnailCheckTarget> {
            requested = limit to recheckAfter
            return targets.take(limit)
        }

        override fun save(target: AttractionThumbnailCheckTarget, missing: Boolean) {
            saved += target to missing
        }
    }
}
