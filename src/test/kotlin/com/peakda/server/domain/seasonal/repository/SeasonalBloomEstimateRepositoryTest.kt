package com.peakda.server.domain.seasonal.repository

import com.peakda.server.domain.auth.application.RefreshTokenService
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.seasonal.entity.Estimator
import com.peakda.server.domain.seasonal.entity.SeasonalBloomEstimate
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
class SeasonalBloomEstimateRepositoryTest {

    @MockitoBean
    lateinit var refreshTokenService: RefreshTokenService

    @MockitoBean
    lateinit var redissonClient: RedissonClient

    @Autowired
    lateinit var repository: SeasonalBloomEstimateRepository

    @BeforeEach
    fun cleanUp() {
        repository.deleteAll()
    }

    @Test
    @Transactional
    fun `산출일 명소 진행 상태와 만개 시작일 창을 모두 만족하는 추정만 조회한다`() {
        repository.saveAllAndFlush(
            listOf(
                estimate(
                    attractionId = 100L,
                    bloomCategory = BloomCategory.CHERRY,
                    peakStartDate = TODAY.plusDays(7),
                ),
                estimate(
                    attractionId = 100L,
                    bloomCategory = BloomCategory.PLUM,
                    peakStartDate = TODAY.plusDays(20),
                ),
                estimate(
                    attractionId = 100L,
                    bloomCategory = BloomCategory.FORSYTHIA,
                    status = BloomStatus.ENDED,
                    peakStartDate = TODAY.plusDays(5),
                ),
                estimate(
                    attractionId = 100L,
                    bloomCategory = BloomCategory.AZALEA,
                    baseDate = BASE_DATE.minusDays(1),
                    peakStartDate = TODAY.plusDays(4),
                ),
                estimate(
                    attractionId = 100L,
                    bloomCategory = BloomCategory.HYDRANGEA,
                    peakStartDate = null,
                ),
                estimate(
                    attractionId = 200L,
                    bloomCategory = BloomCategory.CHERRY,
                    peakStartDate = TODAY.plusDays(6),
                ),
            ),
        )

        val result = repository.findByBaseDateAndAttractionIdInAndStatusNotAndPeakStartDateBetween(
            baseDate = BASE_DATE,
            attractionIds = listOf(100L),
            status = BloomStatus.ENDED,
            peakStartDateStart = TODAY.plusDays(1),
            peakStartDateEnd = TODAY.plusDays(7),
        )

        assertThat(result).hasSize(1)
        val estimate = result.single()
        assertThat(estimate.attractionId).isEqualTo(100L)
        assertThat(estimate.bloomCategory).isEqualTo(BloomCategory.CHERRY)
        assertThat(estimate.baseDate).isEqualTo(BASE_DATE)
        assertThat(estimate.status).isEqualTo(BloomStatus.PREPARING)
        assertThat(estimate.peakStartDate).isEqualTo(TODAY.plusDays(7))
    }

    private fun estimate(
        attractionId: Long,
        bloomCategory: BloomCategory,
        baseDate: LocalDate = BASE_DATE,
        status: BloomStatus = BloomStatus.PREPARING,
        peakStartDate: LocalDate?,
    ) = SeasonalBloomEstimate(
        attractionId = attractionId,
        bloomCategory = bloomCategory,
        baseDate = baseDate,
        status = status,
        confidence = 0.8,
        chosenEstimator = Estimator.CALENDAR,
        peakStartDate = peakStartDate,
    )

    companion object {
        private val TODAY = LocalDate.of(2026, 4, 1)
        private val BASE_DATE = LocalDate.of(2026, 3, 31)

        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16")
            .withDatabaseName("peakda")
            .withUsername("peakda")
            .withPassword("peakda")
    }
}
