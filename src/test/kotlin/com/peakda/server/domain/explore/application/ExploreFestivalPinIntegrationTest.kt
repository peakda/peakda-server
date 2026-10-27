package com.peakda.server.domain.explore.application

import com.peakda.server.common.test.IntegrationTestSupport
import com.peakda.server.domain.curation.application.CurationQueryService
import com.peakda.server.domain.curation.entity.Curation
import com.peakda.server.domain.curation.entity.CurationStatus
import com.peakda.server.domain.curation.repository.CurationRepository
import com.peakda.server.domain.festival.entity.Festival
import com.peakda.server.domain.festival.entity.FestivalEditorial
import com.peakda.server.domain.festival.entity.FestivalEditorialStatus
import com.peakda.server.domain.festival.repository.FestivalEditorialRepository
import com.peakda.server.domain.festival.repository.FestivalRepository
import com.peakda.server.domain.seasonal.entity.BloomCategory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.jdbc.core.JdbcTemplate
import jakarta.persistence.EntityManager
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate

@Transactional
class ExploreFestivalPinIntegrationTest : IntegrationTestSupport() {

    @Autowired
    lateinit var service: ExploreService

    @Autowired
    lateinit var festivalRepository: FestivalRepository

    @Autowired
    lateinit var editorialRepository: FestivalEditorialRepository

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var curationRepository: CurationRepository

    @Autowired
    lateinit var curationQueryService: CurationQueryService

    @Autowired
    lateinit var entityManager: EntityManager

    @Test
    fun `200건 밖 고정 축제도 탐색 카드와 전체 목록에 포함하고 기존 필터는 유지한다`() {
        val normal = (1..200).map { saveFestival("벚꽃 축제 $it", TODAY.minusDays(1), TODAY.plusDays(it.toLong())) }
        val olderPin = Instant.parse("2026-04-01T01:00:00Z")
        saveEditorial(normal[1], olderPin)
        val pinned = saveFestival("벚꽃 고정 축제", TODAY.minusDays(1), TODAY.plusDays(500))
        saveEditorial(pinned, olderPin.plusSeconds(60))
        val excluded = listOf(
            saveFestival("벚꽃 종료 축제", TODAY.minusDays(10), TODAY.minusDays(1)),
            saveFestival("벚꽃 예정 축제", TODAY.plusDays(1), TODAY.plusDays(500)),
            saveFestival("벚꽃 종료일 없는 과거 축제", TODAY.minusDays(1), null),
            saveFestival("벚꽃 날짜 미상 축제", null, null),
            saveFestival("국제 마라톤 대회", TODAY.minusDays(1), TODAY.plusDays(500)),
            saveFestival("매화 고정 축제", TODAY.minusDays(1), TODAY.plusDays(500)),
        )
        excluded.forEach { saveEditorial(it, olderPin.plusSeconds(120)) }
        val draft = saveFestival("벚꽃 초안 축제", TODAY.minusDays(1), TODAY.plusDays(500))
        saveEditorial(draft, olderPin.plusSeconds(180), FestivalEditorialStatus.DRAFT)
        festivalRepository.flush()
        editorialRepository.flush()
        entityManager.clear()

        val all = service.festivals(BloomCategory.CHERRY, TODAY).items.map { it.festivalId }
        val cards = service.explore(null, BloomCategory.CHERRY, TODAY).festivals.map { it.festivalId }

        assertThat(all).hasSize(201)
        assertThat(all.take(4)).containsExactly(pinned.id, normal[1].id, normal[0].id, normal[2].id)
        assertThat(cards).hasSize(10)
        assertThat(cards).containsExactlyElementsOf(all.take(10))
        assertThat(all).doesNotContainAnyElementsOf((excluded + draft).map { requireNotNull(it.id) })
        assertThat(editorialRepository.findByFestivalId(requireNotNull(pinned.id))?.pinnedAt)
            .isEqualTo(olderPin.plusSeconds(60))
    }

    @Test
    fun `탐색과 큐레이션 목록 모두 발행본만 최근 고정순으로 노출한다`() {
        val pinnedAt = Instant.parse("2026-04-01T01:00:00Z")
        val oldPinned = saveCuration(TODAY.minusWeeks(2), CurationStatus.PUBLISHED, pinnedAt)
        val latest = saveCuration(TODAY, CurationStatus.PUBLISHED, null)
        saveCuration(TODAY.plusWeeks(1), CurationStatus.DRAFT, pinnedAt.plusSeconds(60))
        curationRepository.flush()
        entityManager.clear()

        val cards = service.explore(null, null, TODAY).curations
        val firstPage = curationQueryService.cards(PageRequest.of(0, 1))
        val secondPage = curationQueryService.cards(PageRequest.of(1, 1))

        assertThat(cards.map { it.id }).containsExactly(oldPinned.id, latest.id)
        assertThat(firstPage.content.map { it.id }).containsExactly(oldPinned.id)
        assertThat(secondPage.content.map { it.id }).containsExactly(latest.id)
        assertThat(firstPage.totalElements).isEqualTo(2)
        assertThat(curationRepository.findById(requireNotNull(oldPinned.id)).orElseThrow().pinnedAt)
            .isEqualTo(pinnedAt)
    }

    @Test
    fun `고정 시각 컬럼은 두 테이블 모두 nullable timestamptz 이다`() {
        val columns = jdbcTemplate.queryForList(
            """
            SELECT table_name, data_type, is_nullable
            FROM information_schema.columns
            WHERE table_schema = 'public' AND column_name = 'pinned_at'
              AND table_name IN ('festival_editorials', 'curations')
            ORDER BY table_name
            """.trimIndent(),
        )

        assertThat(columns.map { it["table_name"] }).containsExactly("curations", "festival_editorials")
        assertThat(columns.map { it["data_type"] }).containsOnly("timestamp with time zone")
        assertThat(columns.map { it["is_nullable"] }).containsOnly("YES")
    }

    private fun saveFestival(name: String, startsOn: LocalDate?, endsOn: LocalDate?): Festival =
        festivalRepository.save(
            Festival(
                name = name,
                venue = "$name 장소",
                startDate = startsOn?.toString() ?: "미상",
                endDate = endsOn?.toString(),
                startsOn = startsOn,
                endsOn = endsOn,
            ),
        )

    private fun saveEditorial(
        festival: Festival,
        pinnedAt: Instant,
        status: FestivalEditorialStatus = FestivalEditorialStatus.PUBLISHED,
    ) = editorialRepository.save(
        FestivalEditorial(festivalId = requireNotNull(festival.id), status = status, pinnedAt = pinnedAt),
    )

    private fun saveCuration(weekStart: LocalDate, status: CurationStatus, pinnedAt: Instant?): Curation =
        curationRepository.save(
            Curation(
                weekStartDate = weekStart,
                weekEndDate = weekStart.plusDays(6),
                weekLabel = "주간 큐레이션",
                title = "벚꽃을 보러 가요",
                status = status,
                pinnedAt = pinnedAt,
            ),
        )

    companion object {
        private val TODAY = LocalDate.of(2026, 4, 1)
    }
}
