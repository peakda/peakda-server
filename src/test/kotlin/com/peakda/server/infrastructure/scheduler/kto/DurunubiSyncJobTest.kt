package com.peakda.server.infrastructure.scheduler.kto

import com.peakda.server.domain.trail.application.WalkingCourseSyncService
import com.peakda.server.domain.trail.application.WalkingRouteSyncService
import com.peakda.server.domain.trail.repository.WalkingCourseRepository
import com.peakda.server.domain.trail.repository.WalkingRouteRepository
import com.peakda.server.infrastructure.external.kto.durunubi.DurunubiClient
import com.peakda.server.infrastructure.external.kto.durunubi.response.CourseItem
import com.peakda.server.infrastructure.external.kto.durunubi.response.RouteItem
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

class DurunubiSyncJobTest {
    private val fixture = ktoFixture("https://example.test/durunubi", "Durunubi") {
        DurunubiClient(it, testObjectMapper, testErrorDecoder)
    }
    private val routeSync = RecordingRouteSync()
    private val courseSync = RecordingCourseSync()

    @Test
    fun `run 시 routeList와 courseList를 모두 호출한다`() {
        fixture.server.expect(requestTo(startsWith("https://example.test/durunubi/routeList?numOfRows=100&pageNo=1")))
            .andRespond(withSuccess(ROUTE_JSON, MediaType.APPLICATION_JSON))
        fixture.server.expect(requestTo(startsWith("https://example.test/durunubi/courseList?numOfRows=100&pageNo=1")))
            .andRespond(withSuccess(COURSE_JSON, MediaType.APPLICATION_JSON))

        val job = DurunubiSyncJob(fixture.client, routeSync, courseSync, enabled(true), testJobLogger())
        job.run()

        fixture.server.verify()
        assertThat(routeSync.pages.flatten()).extracting<String> { it.routeIdx }.containsExactly("R001")
        assertThat(courseSync.pages.flatten()).extracting<String> { it.crsIdx }.containsExactly("C001")
    }

    @Test
    fun `enabled=false 이면 호출하지 않는다`() {
        val job = DurunubiSyncJob(fixture.client, routeSync, courseSync, enabled(false), testJobLogger())

        job.run()

        fixture.server.verify()
        assertThat(routeSync.pages).isEmpty()
        assertThat(courseSync.pages).isEmpty()
    }

    private fun enabled(jobEnabled: Boolean) = SchedulerProperties(
        enabled = true,
        kto = SchedulerProperties.KtoSchedulerProps(
            durunubi = SchedulerProperties.JobProps(cron = "* * * * * *", enabled = jobEnabled),
        ),
    )

    private class RecordingRouteSync :
        WalkingRouteSyncService(Mockito.mock(WalkingRouteRepository::class.java)) {
        val pages = mutableListOf<List<RouteItem>>()
        override fun upsertPage(items: List<RouteItem>): Int {
            pages += items.toList(); return items.size
        }
    }

    private class RecordingCourseSync :
        WalkingCourseSyncService(Mockito.mock(WalkingCourseRepository::class.java)) {
        val pages = mutableListOf<List<CourseItem>>()
        override fun upsertPage(items: List<CourseItem>): Int {
            pages += items.toList(); return items.size
        }
    }

    companion object {
        private val ROUTE_JSON = """
            { "response": { "header": { "resultCode": "0000", "resultMsg": "OK" },
              "body": { "items": { "item": [ { "routeIdx": "R001", "routeName": "길" } ] }, "totalCount": 1 } } }
        """.trimIndent()
        private val COURSE_JSON = """
            { "response": { "header": { "resultCode": "0000", "resultMsg": "OK" },
              "body": { "items": { "item": [ { "crsIdx": "C001", "crsKorNm": "코스" } ] }, "totalCount": 1 } } }
        """.trimIndent()
    }
}
