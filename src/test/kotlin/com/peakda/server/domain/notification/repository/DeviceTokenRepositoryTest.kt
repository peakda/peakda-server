package com.peakda.server.domain.notification.repository

import com.peakda.server.domain.auth.application.RefreshTokenService
import com.peakda.server.domain.notification.entity.DevicePlatform
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DeviceTokenRepositoryTest {

    @MockitoBean
    lateinit var refreshTokenService: RefreshTokenService

    @Autowired
    lateinit var repository: DeviceTokenRepository

    @Test
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
    fun `deleteExceeding 은 최근 사용 순으로 keep 개만 남긴다`() {
        for (i in 1..5) {
            repository.upsert(1L, "token-$i", DevicePlatform.ANDROID.name)
        }

        val deleted = repository.deleteExceeding(1L, 3)

        assertThat(deleted).isEqualTo(2)
        assertThat(repository.findByUserId(1L)).hasSize(3)
    }

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:18")
            .withDatabaseName("peakda")
            .withUsername("peakda")
            .withPassword("peakda")
    }
}
