package com.peakda.server.domain.notification.application

import com.peakda.server.domain.auth.application.RefreshTokenService
import com.peakda.server.domain.auth.oauth.model.OAuth2LoginType
import com.peakda.server.domain.notification.entity.DevicePlatform
import com.peakda.server.domain.notification.entity.DeviceToken
import com.peakda.server.domain.notification.entity.NotificationLinkType
import com.peakda.server.domain.notification.entity.NotificationType
import com.peakda.server.domain.notification.repository.DeviceTokenRepository
import com.peakda.server.domain.notification.repository.NotificationRepository
import com.peakda.server.domain.user.application.FollowCreatedEvent
import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.repository.UserRepository
import com.peakda.server.infrastructure.push.PushPayload
import com.peakda.server.infrastructure.push.PushSender
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.reset
import org.mockito.Mockito.timeout
import org.mockito.Mockito.verify
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.ApplicationEventPublisher
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class NotificationEventListenerIntegrationTest {

    @MockitoBean
    lateinit var refreshTokenService: RefreshTokenService

    @MockitoBean
    lateinit var redissonClient: RedissonClient

    @MockitoBean
    lateinit var pushSender: PushSender

    @Autowired
    lateinit var eventPublisher: ApplicationEventPublisher

    @Autowired
    lateinit var transactionTemplate: TransactionTemplate

    @Autowired
    lateinit var deviceTokenService: DeviceTokenService

    @Autowired
    lateinit var deviceTokenRepository: DeviceTokenRepository

    @Autowired
    lateinit var notificationRepository: NotificationRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @BeforeEach
    fun cleanUp() {
        notificationRepository.deleteAll()
        deviceTokenRepository.deleteAll()
        userRepository.deleteAll()
        reset(pushSender)
    }

    @Test
    fun `원본 트랜잭션 커밋 후 알림을 저장하고 푸시 실패와 격리한다`() {
        val actor = saveUser("push-actor", "푸시발송자")
        val recipient = saveUser("push-recipient", "푸시수신자")
        val actorId = requireNotNull(actor.id)
        val recipientId = requireNotNull(recipient.id)
        deviceTokenService.register(recipientId, "push-integration-token", DevicePlatform.ANDROID)
        val notificationVisibleWhenPushed = AtomicBoolean(false)
        val pushAttempted = CountDownLatch(1)

        doAnswer {
            notificationVisibleWhenPushed.set(
                notificationRepository.findAll().any { it.recipientId == recipientId },
            )
            pushAttempted.countDown()
            throw IllegalStateException("push failure")
        }.`when`(pushSender).send(anyTokens(), anyPayload())

        transactionTemplate.executeWithoutResult {
            eventPublisher.publishEvent(FollowCreatedEvent(followerId = actorId, followingId = recipientId))
        }

        verify(pushSender, timeout(5_000)).send(anyTokens(), anyPayload())
        assertThat(pushAttempted.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(notificationVisibleWhenPushed).isTrue()
        assertThat(notificationRepository.findAll()).anyMatch { it.recipientId == recipientId }
    }

    private fun saveUser(providerId: String, nickname: String): User =
        userRepository.saveAndFlush(
            User.create(
                provider = OAuth2LoginType.KAKAO,
                providerId = providerId,
                nickname = nickname,
                email = null,
                profileImageUrl = null,
            ),
        )

    private fun anyTokens(): List<DeviceToken> = anyList<DeviceToken>() ?: emptyList()

    private fun anyPayload(): PushPayload = any(PushPayload::class.java) ?: DUMMY_PAYLOAD

    companion object {
        private val DUMMY_PAYLOAD = PushPayload("", "", NotificationLinkType.INTERNAL, null, null, 0L, NotificationType.TIMING)

        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16")
            .withDatabaseName("peakda")
            .withUsername("peakda")
            .withPassword("peakda")
    }
}
