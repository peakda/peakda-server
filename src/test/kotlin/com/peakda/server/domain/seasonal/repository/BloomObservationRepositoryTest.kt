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
import java.time.LocalDate

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class BloomObservationRepositoryTest {

    @MockitoBean
    lateinit var refreshTokenService: RefreshTokenService

    @MockitoBean
    lateinit var redissonClient: RedissonClient

    @Autowired
    lateinit var repository: BloomObservationRepository

    @Autowired
    lateinit var entityManager: EntityManager

    @BeforeEach
    fun cleanUp() {
        repository.deleteAll()
    }

    @Test
    @Transactional
    fun `upsert 는 새 개화 관측을 삽입한다`() {
        repository.upsert(command(floweringOn = LocalDate.of(2026, 3, 29)))
        flushAndClear()

        val observation = repository.findAll().single()
        assertThat(observation.treeType).isEqualTo("벚나무")
        assertThat(observation.obsPlace).isEqualTo("여의도 윤중로")
        assertThat(observation.obsYear).isEqualTo(2026)
        assertThat(observation.floweringOn).isEqualTo(LocalDate.of(2026, 3, 29))
    }

    @Test
    @Transactional
    fun `같은 수종과 장소와 연도를 다시 upsert 하면 행을 추가하지 않고 날짜를 갱신한다`() {
        repository.upsert(command(floweringOn = LocalDate.of(2026, 3, 29)))
        flushAndClear()

        repository.upsert(
            command(
                floweringOn = LocalDate.of(2026, 3, 30),
                fullBloomOn = LocalDate.of(2026, 4, 3),
            ),
        )
        flushAndClear()

        assertThat(repository.count()).isEqualTo(1)
        val observation = repository.findAll().single()
        assertThat(observation.floweringOn).isEqualTo(LocalDate.of(2026, 3, 30))
        assertThat(observation.fullBloomOn).isEqualTo(LocalDate.of(2026, 4, 3))
    }

    private fun command(
        floweringOn: LocalDate,
        fullBloomOn: LocalDate? = null,
    ) = BloomObservationUpsertCommand(
        treeType = "벚나무",
        obsPlace = "여의도 윤중로",
        obsYear = 2026,
        obsPlaceDetail = "영등포구 여의서로",
        flowerStatus = "3",
        buddingOn = LocalDate.of(2026, 3, 25),
        floweringOn = floweringOn,
        fullBloomOn = fullBloomOn,
        sourceModifiedAt = "2026-04-03 18:10:00",
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
