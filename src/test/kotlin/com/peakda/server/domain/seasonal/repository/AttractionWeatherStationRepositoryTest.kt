package com.peakda.server.domain.seasonal.repository

import com.peakda.server.domain.auth.application.RefreshTokenService
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.redisson.api.RedissonClient
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
class AttractionWeatherStationRepositoryTest {

    @MockitoBean
    lateinit var refreshTokenService: RefreshTokenService

    @MockitoBean
    lateinit var redissonClient: RedissonClient

    @Autowired
    lateinit var repository: AttractionWeatherStationRepository

    @Autowired
    lateinit var entityManager: EntityManager

    @BeforeEach
    fun cleanUp() {
        repository.deleteAll()
    }

    @Test
    @Transactional
    fun `upsert 는 새 명소 관측지점 매핑을 삽입한다`() {
        repository.upsert(command(attractionId = 100L, stationId = "108", distanceMeters = 1_250.5))
        flushAndClear()

        val mapping = repository.findAll().single()
        assertThat(mapping.attractionId).isEqualTo(100L)
        assertThat(mapping.stationId).isEqualTo("108")
        assertThat(mapping.distanceMeters).isEqualTo(1_250.5)
    }

    @Test
    @Transactional
    fun `같은 명소를 다시 upsert 하면 행을 추가하지 않고 지점과 거리를 갱신한다`() {
        repository.upsert(command(attractionId = 100L, stationId = "108", distanceMeters = 1_250.5))
        flushAndClear()

        repository.upsert(command(attractionId = 100L, stationId = "112", distanceMeters = 875.2))
        flushAndClear()

        assertThat(repository.count()).isEqualTo(1)
        val mapping = repository.findAll().single()
        assertThat(mapping.stationId).isEqualTo("112")
        assertThat(mapping.distanceMeters).isEqualTo(875.2)
    }

    private fun command(
        attractionId: Long,
        stationId: String,
        distanceMeters: Double,
    ) = AttractionWeatherStationUpsertCommand(
        attractionId = attractionId,
        stationId = stationId,
        distanceMeters = distanceMeters,
    )

    private fun flushAndClear() {
        repository.flush()
        entityManager.clear()
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
