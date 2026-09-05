package com.peakda.server.infrastructure.scheduler.notification

import com.peakda.server.domain.notification.application.BloomTimingAlertService
import com.peakda.server.infrastructure.scheduler.SchedulerProperties
import com.peakda.server.infrastructure.scheduler.testJobLogger
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import java.time.LocalDate

class BloomTimingAlertJobTest {

    private val service = mock(BloomTimingAlertService::class.java)

    @Test
    fun `잡이 활성화되면 오늘 기준 만개 임박 알림을 발송한다`() {
        val job = BloomTimingAlertJob(service, props(jobEnabled = true), testJobLogger())

        job.run()

        verify(service).sendDueAlerts(anyDate())
    }

    @Test
    fun `만개 임박 알림 잡이 비활성화되면 서비스를 호출하지 않는다`() {
        val job = BloomTimingAlertJob(service, props(jobEnabled = false), testJobLogger())

        job.run()

        verifyNoInteractions(service)
    }

    private fun props(jobEnabled: Boolean) = SchedulerProperties(
        enabled = true,
        notification = SchedulerProperties.NotificationSchedulerProps(
            bloomTimingAlert = SchedulerProperties.JobProps(
                cron = "* * * * * *",
                enabled = jobEnabled,
            ),
        ),
    )

    /** Mockito.any() 의 null 반환을 non-null 더미로 감싼다 (매처는 그대로 등록됨). */
    private fun anyDate(): LocalDate = any(LocalDate::class.java) ?: LocalDate.MIN
}
