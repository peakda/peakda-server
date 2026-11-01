package com.peakda.server.domain.festival.repository

import com.peakda.server.common.test.IntegrationTestSupport
import com.peakda.server.domain.festival.entity.Festival
import com.peakda.server.domain.festival.entity.FestivalKind
import com.peakda.server.domain.festival.entity.FestivalSource
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Transactional
class FestivalRepositoryTest : IntegrationTestSupport() {

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
            FestivalKind.FESTIVAL,
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
            FestivalKind.FESTIVAL,
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

    @Test
    fun `진행 중 축제 조회는 요청한 종류만 돌려준다`() {
        repository.saveAllAndFlush(
            listOf(
                festival("봄꽃 축제", LocalDate.of(2026, 3, 30), LocalDate.of(2026, 4, 5)),
                festival("크리스마스 트리", LocalDate.of(2026, 3, 30), LocalDate.of(2026, 4, 5), FestivalKind.TREE),
            ),
        )

        val festivals = repository.findOngoing(FestivalKind.FESTIVAL, LocalDate.of(2026, 4, 1), PageRequest.of(0, 10))
        val trees = repository.findOngoing(FestivalKind.TREE, LocalDate.of(2026, 4, 1), PageRequest.of(0, 10))

        assertThat(festivals.map { it.name }).containsExactly("봄꽃 축제")
        assertThat(trees.map { it.name }).containsExactly("크리스마스 트리")
    }

    @Test
    fun `id 목록 조회는 요청한 종류만 돌려준다`() {
        val saved = repository.saveAllAndFlush(
            listOf(
                festival("봄꽃 축제", LocalDate.of(2026, 3, 30), LocalDate.of(2026, 4, 5)),
                festival("크리스마스 트리", LocalDate.of(2026, 3, 30), LocalDate.of(2026, 4, 5), FestivalKind.TREE),
            ),
        )

        val result = repository.findByIdInAndKind(saved.map { requireNotNull(it.id) }, FestivalKind.FESTIVAL)

        assertThat(result.map { it.name }).containsExactly("봄꽃 축제")
    }

    @Test
    fun `upsert는 같은 키의 직접 등록 행을 덮어쓰지 않는다`() {
        repository.saveAndFlush(
            Festival(
                name = NAME,
                venue = VENUE,
                startDate = START_DATE,
                endDate = "2026-05-03",
                startsOn = LocalDate.of(2026, 5, 1),
                endsOn = LocalDate.of(2026, 5, 3),
                source = FestivalSource.MANUAL,
            ),
        )
        entityManager.clear()

        val written = repository.upsert(command(startsOn = LocalDate.of(2026, 5, 1), endsOn = LocalDate.of(2026, 5, 9)))
        entityManager.flush()
        entityManager.clear()

        val manual = repository.findByNameAndVenueAndStartDate(NAME, VENUE, START_DATE)
        assertThat(written).isZero()
        assertThat(manual?.source).isEqualTo(FestivalSource.MANUAL)
        assertThat(manual?.endsOn).isEqualTo(LocalDate.of(2026, 5, 3))
    }

    @Test
    fun `upsert로 들어온 행은 공공데이터 출처의 축제 종류다`() {
        repository.upsert(command(startsOn = LocalDate.of(2026, 5, 1), endsOn = LocalDate.of(2026, 5, 5)))
        entityManager.flush()
        entityManager.clear()

        val inserted = repository.findByNameAndVenueAndStartDate(NAME, VENUE, START_DATE)
        assertThat(inserted?.source).isEqualTo(FestivalSource.PUBLIC_DATA)
        assertThat(inserted?.kind).isEqualTo(FestivalKind.FESTIVAL)
    }

    @Test
    fun `upsert는 원천 값이 그대로인 재적재에서 행을 쓰지 않는다`() {
        val first = command(startsOn = LocalDate.of(2026, 5, 1), endsOn = LocalDate.of(2026, 5, 5))

        assertThat(repository.upsert(first)).isEqualTo(1)
        assertThat(repository.upsert(first)).isZero()
        assertThat(repository.upsert(first.copy(endDate = null, endsOn = null))).isZero()
        assertThat(repository.upsert(first.copy(homepageUrl = "https://festival.example"))).isEqualTo(1)
    }

    @Test
    fun `sitemap 용 조회는 해당 종류 축제의 id와 수정 시각을 id 오름차순으로 돌려준다`() {
        val saved = repository.saveAllAndFlush(
            listOf(
                festival("첫째", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 2)),
                festival("정규화 실패", null, null),
            ),
        )

        val rows = repository.findByKindOrderByIdAsc(FestivalKind.FESTIVAL)

        assertThat(rows.map { it.id }).containsExactlyElementsOf(saved.map { requireNotNull(it.id) }.sorted())
        assertThat(rows.map { it.updatedAt }).doesNotContainNull()
    }

    private fun festival(
        name: String,
        startsOn: LocalDate?,
        endsOn: LocalDate?,
        kind: FestivalKind = FestivalKind.FESTIVAL,
    ): Festival = Festival(
        name = name,
        venue = "$name 장소",
        startDate = startsOn?.toString() ?: "파싱불가",
        endDate = endsOn?.toString(),
        startsOn = startsOn,
        endsOn = endsOn,
        kind = kind,
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
    }
}
