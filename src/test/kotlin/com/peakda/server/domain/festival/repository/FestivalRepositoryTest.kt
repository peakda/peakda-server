package com.peakda.server.domain.festival.repository

import com.peakda.server.domain.auth.application.RefreshTokenService
import com.peakda.server.domain.festival.entity.Festival
import jakarta.persistence.EntityManager
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
import java.time.LocalDate

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FestivalRepositoryTest {

    @MockitoBean
    lateinit var refreshTokenService: RefreshTokenService

    @MockitoBean
    lateinit var redissonClient: RedissonClient

    @Autowired
    lateinit var repository: FestivalRepository

    @Autowired
    lateinit var entityManager: EntityManager

    @BeforeEach
    fun cleanUp() {
        repository.deleteAll()
    }

    @Test
    fun `오늘 진행 중인 축제만 종료 임박순으로 조회한다`() {
        repository.saveAllAndFlush(
            listOf(
                festival("이미 종료", LocalDate.of(2026, 3, 20), LocalDate.of(2026, 3, 31)),
                festival("종료 임박", LocalDate.of(2026, 3, 30), LocalDate.of(2026, 4, 2)),
                festival("종료 여유", LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 5)),
                festival("종료일 없음 오늘 행사", LocalDate.of(2026, 4, 1), null),
                festival("정규화 실패", null, null),
                festival("아직 시작 전", LocalDate.of(2026, 4, 2), LocalDate.of(2026, 4, 5)),
            ),
        )

        val result = repository.findOngoing(
            LocalDate.of(2026, 4, 1),
            PageRequest.of(0, 10),
        )

        assertThat(result.map { it.name }).containsExactly("종료일 없음 오늘 행사", "종료 임박", "종료 여유")
    }

    @Test
    fun `진행 중 축제 조회에 pageable 상한을 적용한다`() {
        repository.saveAllAndFlush(
            listOf(
                festival("첫째", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 1)),
                festival("둘째", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 2)),
                festival("셋째", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 3)),
            ),
        )

        val result = repository.findOngoing(
            LocalDate.of(2026, 4, 1),
            PageRequest.of(0, 2),
        )

        assertThat(result.map { it.name }).containsExactly("첫째", "둘째")
    }

    @Test
    fun `upsert는 정규화 날짜를 적재하고 같은 축제 재적재 시 종료일을 갱신한다`() {
        repository.upsert(
            command(
                startsOn = LocalDate.of(2026, 5, 1),
                endsOn = LocalDate.of(2026, 5, 5),
            ),
        )
        entityManager.flush()
        entityManager.clear()

        val inserted = repository.findByNameAndVenueAndStartDate(NAME, VENUE, START_DATE)
        assertThat(inserted?.startsOn).isEqualTo(LocalDate.of(2026, 5, 1))
        assertThat(inserted?.endsOn).isEqualTo(LocalDate.of(2026, 5, 5))

        repository.upsert(
            command(
                startsOn = LocalDate.of(2026, 5, 1),
                endsOn = LocalDate.of(2026, 5, 7),
            ),
        )
        entityManager.flush()
        entityManager.clear()

        val updated = repository.findByNameAndVenueAndStartDate(NAME, VENUE, START_DATE)
        assertThat(updated?.startsOn).isEqualTo(LocalDate.of(2026, 5, 1))
        assertThat(updated?.endsOn).isEqualTo(LocalDate.of(2026, 5, 7))
    }

    private fun festival(name: String, startsOn: LocalDate?, endsOn: LocalDate?): Festival = Festival(
        name = name,
        venue = "$name 장소",
        startDate = startsOn?.toString() ?: "파싱불가",
        endDate = endsOn?.toString(),
        startsOn = startsOn,
        endsOn = endsOn,
    )

    private fun command(startsOn: LocalDate, endsOn: LocalDate): FestivalUpsertCommand = FestivalUpsertCommand(
        name = NAME,
        venue = VENUE,
        startDate = START_DATE,
        endDate = endsOn.toString(),
        startsOn = startsOn,
        endsOn = endsOn,
        hostOrganization = null,
        organizingInstitution = null,
        supportingInstitution = null,
        phoneNumber = null,
        homepageUrl = null,
        roadAddress = null,
        landLotAddress = null,
        latitude = null,
        longitude = null,
        referenceDate = null,
        providerInstitutionCode = null,
        providerInstitutionName = null,
    )

    companion object {
        private const val NAME = "봄꽃 축제"
        private const val VENUE = "중앙광장"
        private const val START_DATE = "2026-05-01"

        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16")
            .withDatabaseName("peakda")
            .withUsername("peakda")
            .withPassword("peakda")
    }
}
