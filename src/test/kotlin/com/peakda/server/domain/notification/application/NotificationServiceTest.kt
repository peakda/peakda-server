package com.peakda.server.domain.notification.application

import com.peakda.server.common.page.PageRequest
import com.peakda.server.domain.notification.entity.Notification
import com.peakda.server.domain.notification.entity.NotificationLinkType
import com.peakda.server.domain.notification.entity.NotificationSegment
import com.peakda.server.domain.notification.entity.NotificationType
import com.peakda.server.domain.notification.exception.NotificationNotFoundException
import com.peakda.server.domain.notification.repository.NotificationRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageImpl
import org.springframework.test.util.ReflectionTestUtils
import java.time.Instant
import org.springframework.data.domain.PageRequest as SpringPageRequest

class NotificationServiceTest {

    private val notificationRepository = mock(NotificationRepository::class.java)
    private val service = NotificationService(notificationRepository)

    private val pageable = SpringPageRequest.of(0, 20)

    @Test
    fun `ALL 세그먼트는 타입 필터 없이 조회한다`() {
        `when`(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(RECIPIENT_ID, pageable))
            .thenReturn(PageImpl(listOf(notification(1L, NotificationType.FOLLOW))))

        val response = service.list(RECIPIENT_ID, NotificationSegment.ALL, PageRequest())

        assertThat(response.content).extracting<Long> { it.id }.containsExactly(1L)
    }

    @Test
    fun `ACTIVITY 세그먼트는 팔로우·리액션 타입으로 필터한다`() {
        `when`(
            notificationRepository.findByRecipientIdAndTypeInOrderByCreatedAtDesc(
                RECIPIENT_ID,
                listOf(NotificationType.FOLLOW, NotificationType.REACTION),
                pageable,
            ),
        ).thenReturn(PageImpl(listOf(notification(2L, NotificationType.REACTION))))

        val response = service.list(RECIPIENT_ID, NotificationSegment.ACTIVITY, PageRequest())

        assertThat(response.content).extracting<NotificationType> { it.type }.containsExactly(NotificationType.REACTION)
    }

    @Test
    fun `안 읽은 알림 개수를 반환한다`() {
        `when`(notificationRepository.countByRecipientIdAndReadAtIsNull(RECIPIENT_ID)).thenReturn(3L)

        assertThat(service.unreadCount(RECIPIENT_ID)).isEqualTo(3L)
    }

    @Test
    fun `본인 알림이면 읽음 처리된다`() {
        val notification = notification(5L, NotificationType.FOLLOW)
        `when`(notificationRepository.findByIdAndRecipientId(5L, RECIPIENT_ID)).thenReturn(notification)

        service.markRead(RECIPIENT_ID, 5L)

        assertThat(notification.readAt).isNotNull()
    }

    @Test
    fun `본인 알림이 아니면 NotificationNotFoundException 이다`() {
        `when`(notificationRepository.findByIdAndRecipientId(5L, RECIPIENT_ID)).thenReturn(null)

        assertThatThrownBy { service.markRead(RECIPIENT_ID, 5L) }
            .isInstanceOf(NotificationNotFoundException::class.java)
    }

    @Test
    fun `전체 읽음 처리를 위임한다`() {
        service.markAllRead(RECIPIENT_ID)

        verify(notificationRepository).markAllRead(eq(RECIPIENT_ID), anyInstant())
    }

    private fun notification(id: Long, type: NotificationType): Notification {
        val notification = Notification(
            recipientId = RECIPIENT_ID,
            type = type,
            title = "제목",
            body = "본문",
            linkType = NotificationLinkType.INTERNAL,
        )
        ReflectionTestUtils.setField(notification, "id", id)
        ReflectionTestUtils.setField(notification, "createdAt", Instant.now())
        return notification
    }

    /** Mockito any() 는 null 을 돌려주므로 Kotlin non-null 파라미터용으로 non-null 더미로 감싼다 (매처는 그대로 등록됨). */
    private fun anyInstant(): Instant = org.mockito.ArgumentMatchers.any(Instant::class.java) ?: Instant.EPOCH

    companion object {
        private const val RECIPIENT_ID = 42L
    }
}
