package com.peakda.server.domain.notification.repository

import com.peakda.server.domain.auth.application.RefreshTokenService
import com.peakda.server.domain.auth.oauth.model.OAuth2LoginType
import com.peakda.server.domain.notification.application.DeviceTokenService
import com.peakda.server.domain.notification.entity.DevicePlatform
import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class DeviceTokenRepositoryTest {

    @MockitoBean
    lateinit var refreshTokenService: RefreshTokenService

    @MockitoBean
    lateinit var redissonClient: RedissonClient

    @Autowired
    lateinit var repository: DeviceTokenRepository

    @Autowired
    lateinit var service: DeviceTokenService

    @Autowired
    lateinit var userRepository: UserRepository

    @BeforeEach
    fun cleanUp() {
        repository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    @Transactional
    fun `upsert 는 새 토큰을 삽입하고 같은 토큰 재등록 시 소유자와 플랫폼을 갱신한다`() {
        repository.upsert(1L, "token-a", DevicePlatform.ANDROID.name)
        repository.upsert(2L, "token-a", DevicePlatform.IOS.name)

        val tokens = repository.findAll().filter { it.token == "token-a" }
        assertThat(tokens).hasSize(1)
        assertThat(tokens.single().userId).isEqualTo(2L)
        assertThat(tokens.single().platform).isEqualTo(DevicePlatform.IOS)
        assertThat(repository.findByUserId(1L)).isEmpty()
    }

    @Test
    @Transactional
    fun `deleteExceeding 은 최근 사용 순으로 keep 개만 남긴다`() {
        for (i in 1..5) {
            repository.upsert(1L, "token-$i", DevicePlatform.ANDROID.name)
        }

        val deleted = repository.deleteExceeding(1L, 3)

        assertThat(deleted).isEqualTo(2)
        assertThat(repository.findByUserId(1L).map { it.token })
            .containsExactlyInAnyOrder("token-3", "token-4", "token-5")
    }

    @Test
    @Transactional
    fun `1024바이트를 초과한 토큰은 DB 제약으로 거부한다`() {
        assertThatThrownBy {
            repository.upsert(1L, "a".repeat(1025), DevicePlatform.ANDROID.name)
        }.isInstanceOf(DataIntegrityViolationException::class.java)
    }

    @Test
    fun `동시에 등록해도 사용자별 토큰은 10개만 남는다`() {
        val user = userRepository.saveAndFlush(
            User.create(
                provider = OAuth2LoginType.KAKAO,
                providerId = "device-token-concurrency-user",
                nickname = "동시성토큰사용자",
                email = null,
                profileImageUrl = null,
            ),
        )
        val userId = requireNotNull(user.id)
        val taskCount = 20
        val ready = CountDownLatch(taskCount)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(taskCount)

        try {
            val futures = (1..taskCount).map { sequence ->
                executor.submit {
                    ready.countDown()
                    start.await()
                    service.register(userId, "concurrent-token-$sequence", DevicePlatform.ANDROID)
                }
            }

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue()
            start.countDown()
            futures.forEach { it.get(10, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }

        assertThat(repository.findByUserId(userId)).hasSize(10)
    }

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16")
            .withDatabaseName("peakda")
            .withUsername("peakda")
            .withPassword("peakda")
    }
}
