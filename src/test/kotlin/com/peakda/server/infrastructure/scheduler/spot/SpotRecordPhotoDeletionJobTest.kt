package com.peakda.server.infrastructure.scheduler.spot

import com.peakda.server.domain.spot.application.SpotRecordPhotoDeletionProperties
import com.peakda.server.domain.spot.application.SpotRecordPhotoDeletionService
import com.peakda.server.infrastructure.scheduler.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

class SpotRecordPhotoDeletionJobTest {
    private val service = mock(SpotRecordPhotoDeletionService::class.java)

    @Test fun `전체 또는 사진 잡 비활성화는 DB 작업도 실행하지 않는다`() {
        SpotRecordPhotoDeletionJob(service, SpotRecordPhotoDeletionProperties(), SchedulerProperties(enabled = false), testJobLogger()).run()
        SpotRecordPhotoDeletionJob(service, SpotRecordPhotoDeletionProperties(enabled = false), SchedulerProperties(enabled = true), testJobLogger()).run()
        verifyNoInteractions(service)
    }

    @Test fun `자동 실행은 domain 처리와 적체 관측을 기존 잡 경계로 위임한다`() {
        `when`(service.drain()).thenReturn(3)
        `when`(service.outstandingCounts()).thenReturn(mapOf("FAILED" to 2L))
        SpotRecordPhotoDeletionJob(service, SpotRecordPhotoDeletionProperties(), SchedulerProperties(enabled = true), testJobLogger()).run()
        verify(service).drain()
        verify(service).outstandingCounts()
    }

    @Test fun `수동 실행은 전역 비활성 상태에서도 기존 레지스트리로 호출 가능하다`() {
        val job = SpotRecordPhotoDeletionJob(service, SpotRecordPhotoDeletionProperties(), SchedulerProperties(), testJobLogger())
        val registry = ManualJobRegistry(listOf(job))
        registry.find("spotRecordPhotoDeletion")!!.runNow()
        verify(service).drain()
        verify(service).outstandingCounts()
        assertThat(registry.names()).contains("spotRecordPhotoDeletion")
    }
    @Test fun `FAILED 복구는 기존 수동 잡 레지스트리에서 명시적으로 재예약 후 처리한다`() {
        val job = SpotRecordPhotoDeletionRetryJob(service, testJobLogger())
        ManualJobRegistry(listOf(job)).find("spotRecordPhotoDeletionRetryFailed")!!.runNow()
        val order = inOrder(service)
        order.verify(service).retryFailedBatch()
        order.verify(service).drain()
        order.verify(service).outstandingCounts()
    }

}
