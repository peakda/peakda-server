package com.peakda.server.domain.location.repository

import com.peakda.server.domain.auth.application.RefreshTokenService
import com.peakda.server.domain.location.entity.LocationAccessChannel
import com.peakda.server.domain.location.entity.LocationServiceType
import com.peakda.server.domain.location.entity.LocationUsageLog
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant

/**
 * 선택 조건을 `COALESCE` 로 편 쿼리가 PostgreSQL 에서 실제로 도는지까지 확인한다.
 * 파라미터 타입 추론 실패(SQLState 42P18)는 H2 나 목으로는 드러나지 않는다.
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class LocationUsageLogRepositoryTest {

    @MockitoBean
    lateinit var refreshTokenService: RefreshTokenService

    @MockitoBean
    lateinit var redissonClient: RedissonClient

    @Autowired
    lateinit var repository: LocationUsageLogRepository

    private val pageable = PageRequest.of(0, 20)

    @BeforeEach
    fun cleanUp() {
        repository.deleteAll()
    }

    @Test
    @Transactional
    fun `조건이 없으면 전체를 최신순으로 조회한다`() {
        repository.saveAllAndFlush(
            listOf(
                usageLog(userId = 1L, service = LocationServiceType.BLOOM_MAP, usedAt = FIRST),
                usageLog(userId = 2L, service = LocationServiceType.SPOT_MATCH, usedAt = SECOND),
            ),
        )

        val result = repository.search(null, null, null, pageable)

        assertThat(result.content).hasSize(2)
        assertThat(result.content.map { it.userId }).containsExactly(2L, 1L)
    }

    @Test
    @Transactional
    fun `제공서비스로 좁혀 조회한다`() {
        repository.saveAllAndFlush(
            listOf(
                usageLog(userId = 1L, service = LocationServiceType.BLOOM_MAP, usedAt = FIRST),
                usageLog(userId = 2L, service = LocationServiceType.SPOT_MATCH, usedAt = SECOND),
            ),
        )

        val result = repository.search(LocationServiceType.SPOT_MATCH, null, null, pageable)

        assertThat(result.content).singleElement()
            .satisfies({ assertThat(it.userId).isEqualTo(2L) })
    }

    @Test
    @Transactional
    fun `이용일시 구간으로 좁혀 조회한다`() {
        repository.saveAllAndFlush(
            listOf(
                usageLog(userId = 1L, service = LocationServiceType.BLOOM_MAP, usedAt = FIRST),
                usageLog(userId = 2L, service = LocationServiceType.BLOOM_MAP, usedAt = SECOND),
                usageLog(userId = 3L, service = LocationServiceType.BLOOM_MAP, usedAt = THIRD),
            ),
        )

        val result = repository.search(null, SECOND, SECOND, pageable)

        assertThat(result.content).singleElement()
            .satisfies({ assertThat(it.userId).isEqualTo(2L) })
    }

    @Test
    @Transactional
    fun `시작 일시만 지정해도 종료 조건 없이 조회한다`() {
        repository.saveAllAndFlush(
            listOf(
                usageLog(userId = 1L, service = LocationServiceType.BLOOM_MAP, usedAt = FIRST),
                usageLog(userId = 2L, service = LocationServiceType.BLOOM_MAP, usedAt = THIRD),
            ),
        )

        val result = repository.search(null, SECOND, null, pageable)

        assertThat(result.content).singleElement()
            .satisfies({ assertThat(it.userId).isEqualTo(2L) })
    }

    @Test
    @Transactional
    fun `대상 사용자와 제공서비스를 함께 걸어 조회한다`() {
        repository.saveAllAndFlush(
            listOf(
                usageLog(userId = 1L, service = LocationServiceType.BLOOM_MAP, usedAt = FIRST),
                usageLog(userId = 1L, service = LocationServiceType.SPOT_MATCH, usedAt = SECOND),
                usageLog(userId = 2L, service = LocationServiceType.BLOOM_MAP, usedAt = THIRD),
            ),
        )

        val result = repository.searchByUserIds(
            listOf(1L),
            LocationServiceType.BLOOM_MAP,
            null,
            null,
            pageable,
        )

        assertThat(result.content).singleElement().satisfies({
            assertThat(it.userId).isEqualTo(1L)
            assertThat(it.service).isEqualTo(LocationServiceType.BLOOM_MAP)
        })
    }

    private fun usageLog(
        userId: Long,
        service: LocationServiceType,
        usedAt: Instant,
        channel: LocationAccessChannel = LocationAccessChannel.ANDROID,
    ): LocationUsageLog = LocationUsageLog(
        userId = userId,
        channel = channel,
        service = service,
        usedAt = usedAt,
    )

    companion object {
        private val FIRST: Instant = Instant.parse("2026-08-01T00:00:00Z")
        private val SECOND: Instant = Instant.parse("2026-08-10T00:00:00Z")
        private val THIRD: Instant = Instant.parse("2026-08-20T00:00:00Z")

        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16")
            .withDatabaseName("peakda")
            .withUsername("peakda")
            .withPassword("peakda")
    }
}
