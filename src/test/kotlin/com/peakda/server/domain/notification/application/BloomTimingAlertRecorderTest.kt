package com.peakda.server.domain.notification.application

import com.peakda.server.domain.notification.entity.Notification
import com.peakda.server.domain.notification.entity.NotificationLinkType
import com.peakda.server.domain.notification.entity.NotificationType
import com.peakda.server.domain.notification.repository.BloomTimingAlertRepository
import com.peakda.server.domain.seasonal.entity.BloomCategory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.LocalDate

class BloomTimingAlertRecorderTest {

    private val repository = mock(BloomTimingAlertRepository::class.java)
    private val notificationService = mock(NotificationService::class.java)
    private val recorder = BloomTimingAlertRecorder(repository, notificationService)
    private val commandCaptor: ArgumentCaptor<CreateNotificationCommand> =
        ArgumentCaptor.forClass(CreateNotificationCommand::class.java)

    @Test
    fun `새 발송 키면 TIMING 알림을 생성하고 반환한다`() {
        val candidate = candidate()
        val notification = notification()
        `when`(
            repository.insertIfAbsent(
                candidate.userId,
                candidate.spotId,
                candidate.bloomCategory.name,
                candidate.peakYear,
                candidate.peakStartDate,
            ),
        ).thenReturn(1)
        `when`(notificationService.create(captureCommand())).thenReturn(notification)

        val result = recorder.record(candidate)

        assertThat(result).isSameAs(notification)
        val command = commandCaptor.value
        assertThat(command.recipientId).isEqualTo(candidate.userId)
        assertThat(command.type).isEqualTo(NotificationType.TIMING)
        assertThat(command.targetId).isEqualTo(candidate.spotId)
        assertThat(command.linkType).isEqualTo(NotificationLinkType.INTERNAL)
        assertThat(command.body)
            .contains(candidate.spotName)
            .contains(candidate.bloomCategory.displayName)
            .contains(candidate.daysUntilPeak.toString())
    }

    @Test
    fun `이미 발송한 키면 알림을 생성하지 않고 null을 반환한다`() {
        val candidate = candidate()
        `when`(
            repository.insertIfAbsent(
                candidate.userId,
                candidate.spotId,
                candidate.bloomCategory.name,
                candidate.peakYear,
                candidate.peakStartDate,
            ),
        ).thenReturn(0)

        assertThat(recorder.record(candidate)).isNull()

        verify(notificationService, never()).create(captureCommand())
    }

    /** ArgumentCaptor.capture() 도 null 을 돌려주므로 non-null 더미로 감싼다 (매처는 그대로 등록됨). */
    private fun captureCommand(): CreateNotificationCommand = commandCaptor.capture() ?: DUMMY_COMMAND

    private fun candidate() = BloomTimingAlertCandidate(
        userId = 1L,
        spotId = 10L,
        spotName = "서울숲",
        bloomCategory = BloomCategory.CHERRY,
        peakStartDate = LocalDate.of(2026, 4, 8),
        daysUntilPeak = 7,
    )

    private fun notification() = Notification(
        recipientId = 1L,
        type = NotificationType.TIMING,
        title = BloomTimingAlertMessage.TITLE,
        body = BloomTimingAlertMessage.body(candidate()),
        linkType = NotificationLinkType.INTERNAL,
        targetId = 10L,
    )

    companion object {
        private val DUMMY_COMMAND = CreateNotificationCommand(0L, NotificationType.TIMING, "", "")
    }
}
