package com.peakda.server.domain.weather.repository

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
import java.time.LocalDate

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class WeatherDailyObservationRepositoryTest {

    @MockitoBean
    lateinit var refreshTokenService: RefreshTokenService

    @MockitoBean
    lateinit var redissonClient: RedissonClient

    @Autowired
    lateinit var repository: WeatherDailyObservationRepository

    @Autowired
    lateinit var entityManager: EntityManager

    @BeforeEach
    fun cleanUp() {
        repository.deleteAll()
    }

    @Test
    @Transactional
    fun `upsert 는 새 관측을 삽입한다`() {
        repository.upsert(
            command(
                stationId = "108",
                observedOn = LocalDate.of(2026, 1, 15),
                stationName = "서울",
                avgTemperature = -1.2,
                minTemperature = -5.7,
                maxTemperature = 3.4,
            ),
        )
        flushAndClear()

        val observation = repository.findAll().single()
        assertThat(observation.stationId).isEqualTo("108")
        assertThat(observation.observedOn).isEqualTo(LocalDate.of(2026, 1, 15))
        assertThat(observation.stationName).isEqualTo("서울")
        assertThat(observation.avgTemperature).isEqualTo(-1.2)
        assertThat(observation.minTemperature).isEqualTo(-5.7)
        assertThat(observation.maxTemperature).isEqualTo(3.4)
    }

    @Test
    @Transactional
    fun `같은 지점과 관측일을 다시 upsert 하면 행을 추가하지 않고 값을 갱신한다`() {
        repository.upsert(
            command(
                stationId = "108",
                observedOn = LocalDate.of(2026, 1, 15),
                stationName = "서울",
                avgTemperature = -1.2,
                minTemperature = -5.7,
                maxTemperature = 3.4,
            ),
        )
        flushAndClear()

        repository.upsert(
            command(
                stationId = "108",
                observedOn = LocalDate.of(2026, 1, 15),
                stationName = "서울특별시",
                avgTemperature = 0.8,
                minTemperature = -3.1,
                maxTemperature = 5.6,
            ),
        )
        flushAndClear()

        assertThat(repository.count()).isEqualTo(1)
        val observation = repository.findAll().single()
        assertThat(observation.stationName).isEqualTo("서울특별시")
        assertThat(observation.avgTemperature).isEqualTo(0.8)
        assertThat(observation.minTemperature).isEqualTo(-3.1)
        assertThat(observation.maxTemperature).isEqualTo(5.6)
    }

    @Test
    @Transactional
    fun `기온이 모두 null 인 관측도 저장한다`() {
        repository.upsert(
            command(
                stationId = "159",
                observedOn = LocalDate.of(2026, 1, 20),
                stationName = "부산",
                avgTemperature = null,
                minTemperature = null,
                maxTemperature = null,
            ),
        )
        flushAndClear()

        val observation = repository.findAll().single()
        assertThat(observation.avgTemperature).isNull()
        assertThat(observation.minTemperature).isNull()
        assertThat(observation.maxTemperature).isNull()
    }

    @Test
    @Transactional
    fun `지점별 최신 관측일을 조회한다`() {
        listOf(
            command("108", LocalDate.of(2026, 1, 1)),
            command("108", LocalDate.of(2026, 1, 3)),
            command("108", LocalDate.of(2026, 1, 2)),
            command("159", LocalDate.of(2026, 2, 5)),
            command("159", LocalDate.of(2026, 2, 1)),
        ).forEach { repository.upsert(it) }
        flushAndClear()

        val latestByStation = repository.findLatestObservedOnByStation()
            .associate { it.stationId to it.latestObservedOn }

        assertThat(latestByStation).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                "108" to LocalDate.of(2026, 1, 3),
                "159" to LocalDate.of(2026, 2, 5),
            ),
        )
    }

    @Test
    @Transactional
    fun `관측이 하나도 없으면 지점별 최신 관측일 조회는 빈 결과를 돌려준다`() {
        val result = repository.findLatestObservedOnByStation()

        assertThat(result).isEmpty()
    }

    private fun command(
        stationId: String,
        observedOn: LocalDate,
        stationName: String? = null,
        avgTemperature: Double? = null,
        minTemperature: Double? = null,
        maxTemperature: Double? = null,
    ) = WeatherDailyObservationUpsertCommand(
        stationId = stationId,
        observedOn = observedOn,
        stationName = stationName,
        avgTemperature = avgTemperature,
        minTemperature = minTemperature,
        maxTemperature = maxTemperature,
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
