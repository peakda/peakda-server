package com.peakda.server.domain.notification.application

import com.peakda.server.domain.auth.oauth.model.OAuth2LoginType
import com.peakda.server.domain.feed.application.ReactionAddedEvent
import com.peakda.server.domain.notification.entity.NotificationType
import com.peakda.server.domain.spot.entity.ReactionType
import com.peakda.server.domain.user.application.FollowCreatedEvent
import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.test.util.ReflectionTestUtils
import java.util.Optional

class NotificationEventListenerTest {

    private val notificationService = mock(NotificationService::class.java)
    private val userRepository = mock(UserRepository::class.java)
    private val listener = NotificationEventListener(notificationService, userRepository)

    private val captor: ArgumentCaptor<CreateNotificationCommand> =
        ArgumentCaptor.forClass(CreateNotificationCommand::class.java)

    @Test
    fun `본인이 남긴 리액션이면 알림을 생성하지 않는다`() {
        listener.onReactionAdded(ReactionAddedEvent(actorId = 1L, recordId = 100L, recordOwnerId = 1L, reactionType = ReactionType.HEART))

        verify(notificationService, never()).create(capture())
    }

    @Test
    fun `타인의 리액션이면 기록 작성자에게 REACTION 알림을 생성한다`() {
        `when`(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "반응러")))

        listener.onReactionAdded(ReactionAddedEvent(actorId = 2L, recordId = 100L, recordOwnerId = 9L, reactionType = ReactionType.HEART))

        verify(notificationService).create(capture())
        assertThat(captor.value.recipientId).isEqualTo(9L)
        assertThat(captor.value.type).isEqualTo(NotificationType.REACTION)
        assertThat(captor.value.targetId).isEqualTo(100L)
    }

    @Test
    fun `팔로우가 맺어지면 팔로우 대상에게 FOLLOW 알림을 생성한다`() {
        `when`(userRepository.findById(3L)).thenReturn(Optional.of(user(3L, "팔로워")))

        listener.onFollowCreated(FollowCreatedEvent(followerId = 3L, followingId = 7L))

        verify(notificationService).create(capture())
        assertThat(captor.value.recipientId).isEqualTo(7L)
        assertThat(captor.value.type).isEqualTo(NotificationType.FOLLOW)
        assertThat(captor.value.targetId).isEqualTo(3L)
    }

    /** ArgumentCaptor.capture() 도 null 을 돌려주므로 non-null 더미로 감싼다 (매처는 그대로 등록됨). */
    private fun capture(): CreateNotificationCommand = captor.capture() ?: DUMMY

    private fun user(id: Long, nickname: String): User {
        val user = User(
            provider = OAuth2LoginType.KAKAO,
            providerId = "p-$id",
            nickname = nickname,
            profileImageUrl = null,
        )
        ReflectionTestUtils.setField(user, "id", id)
        return user
    }

    companion object {
        private val DUMMY = CreateNotificationCommand(0L, NotificationType.TIMING, "", "")
    }
}
