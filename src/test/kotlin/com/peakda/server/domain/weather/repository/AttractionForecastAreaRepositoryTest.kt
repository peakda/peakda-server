package com.peakda.server.domain.weather.repository

import com.peakda.server.domain.auth.application.RefreshTokenService
import org.assertj.core.api.Assertions.assertThat
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

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AttractionForecastAreaRepositoryTest {

    @MockitoBean
    lateinit var refreshTokenService: RefreshTokenService

    @MockitoBean
    lateinit var redissonClient: RedissonClient

    @Autowired
    lateinit var repository: AttractionForecastAreaRepository

    @Test
    fun `같은 명소를 다시 매핑하면 격자를 갱신하고, 격자는 명소가 많은 순으로 상한만큼 읽는다`() {
        repository.upsert(AttractionForecastAreaUpsertCommand(1, 60, 127, "SEOUL"))
        repository.upsert(AttractionForecastAreaUpsertCommand(2, 91, 106, "GYEONGNAM"))
        repository.upsert(AttractionForecastAreaUpsertCommand(3, 91, 106, "GYEONGNAM"))
        repository.upsert(AttractionForecastAreaUpsertCommand(4, 52, 38, "JEJU"))
        repository.upsert(AttractionForecastAreaUpsertCommand(1, 91, 106, "GYEONGNAM"))

        val grids = repository.findGridsByAttractionCount(PageRequest.of(0, 2))

        assertThat(grids).containsExactly(ForecastGrid(91, 106, 3), ForecastGrid(52, 38, 1))
        assertThat(repository.findByAttractionId(1)?.midRegionCode).isEqualTo("GYEONGNAM")
    }

    @Test
    fun `대상 명소 밖의 매핑만 지운다`() {
        repository.upsert(AttractionForecastAreaUpsertCommand(1, 60, 127, "SEOUL"))
        repository.upsert(AttractionForecastAreaUpsertCommand(2, 91, 106, "GYEONGNAM"))

        val removed = repository.deleteByAttractionIdNotIn(setOf(1L))

        assertThat(removed).isEqualTo(1)
        assertThat(repository.findAll()).extracting<Long> { it.attractionId }.containsExactly(1L)
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
