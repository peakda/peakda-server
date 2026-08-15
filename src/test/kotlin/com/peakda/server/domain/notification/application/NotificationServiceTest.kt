package com.peakda.server.domain.notification.application

import com.peakda.server.common.storage.ObjectKeyUrlResolver
import com.peakda.server.common.storage.ObjectStorage
import com.peakda.server.domain.auth.oauth.model.OAuth2LoginType
import com.peakda.server.common.page.PageRequest
import com.peakda.server.domain.notification.entity.Notification
import com.peakda.server.domain.notification.entity.NotificationLinkType
import com.peakda.server.domain.notification.entity.NotificationSegment
import com.peakda.server.domain.notification.entity.NotificationType
import com.peakda.server.domain.notification.exception.NotificationNotFoundException
import com.peakda.server.domain.notification.repository.NotificationRepository
import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.repository.UserRepository
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
    private val userRepository = mock(UserRepository::class.java)
    private val objectStorage = mock(ObjectStorage::class.java)
    private val service = NotificationService(
        notificationRepository,
        userRepository,
        ObjectKeyUrlResolver(objectStorage),
    )

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
    fun `행위자 이미지는 목록의 사용자 id를 한 번에 조회한다`() {
        val first = notification(2L, NotificationType.FOLLOW, actorUserId = 7L)
        val second = notification(3L, NotificationType.REACTION, actorUserId = 8L)
        `when`(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(RECIPIENT_ID, pageable))
            .thenReturn(PageImpl(listOf(first, second)))
        `when`(userRepository.findAllById(listOf(7L, 8L))).thenReturn(listOf(user(7L, "첫 사용자", "profile-7"), user(8L, "둘 사용자", "profile-8")))
        `when`(objectStorage.presignedGetUrl("profile-7")).thenReturn("https://cdn/7")
        `when`(objectStorage.presignedGetUrl("profile-8")).thenReturn("https://cdn/8")

        val response = service.list(RECIPIENT_ID, NotificationSegment.ALL, PageRequest())

        assertThat(response.content).extracting<String?> { it.imageUrl }.containsExactly("https://cdn/7", "https://cdn/8")
        verify(userRepository).findAllById(listOf(7L, 8L))
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

    private fun notification(id: Long, type: NotificationType, actorUserId: Long? = null): Notification {
        val notification = Notification(
            recipientId = RECIPIENT_ID,
            actorUserId = actorUserId,
            type = type,
            title = "제목",
            body = "본문",
            linkType = NotificationLinkType.INTERNAL,
        )
        ReflectionTestUtils.setField(notification, "id", id)
        ReflectionTestUtils.setField(notification, "createdAt", Instant.now())
        return notification
    }

    private fun user(id: Long, nickname: String, profileImageUrl: String): User {
        val user = User(
            provider = OAuth2LoginType.KAKAO,
            providerId = "provider-$id",
            nickname = nickname,
            profileImageUrl = profileImageUrl,
        )
        ReflectionTestUtils.setField(user, "id", id)
        return user
    }

    /** Mockito any() 는 null 을 돌려주므로 Kotlin non-null 파라미터용으로 non-null 더미로 감싼다 (매처는 그대로 등록됨). */
    private fun anyInstant(): Instant = org.mockito.ArgumentMatchers.any(Instant::class.java) ?: Instant.EPOCH

    companion object {
        private const val RECIPIENT_ID = 42L
    }
}
