package com.peakda.server.domain.festival.application

import com.peakda.server.domain.festival.entity.Festival
import com.peakda.server.domain.festival.entity.FestivalEditorial
import com.peakda.server.domain.festival.entity.FestivalEditorialStatus
import com.peakda.server.domain.festival.entity.FestivalHighlight
import com.peakda.server.domain.festival.exception.FestivalNotFoundException
import com.peakda.server.domain.festival.repository.FestivalEditorialRepository
import com.peakda.server.domain.festival.repository.FestivalHighlightRepository
import com.peakda.server.domain.festival.repository.FestivalRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate
import java.util.Optional

class FestivalDetailServiceTest {

    private val festivalRepository = mock(FestivalRepository::class.java)
    private val festivalEditorialRepository = mock(FestivalEditorialRepository::class.java)
    private val festivalHighlightRepository = mock(FestivalHighlightRepository::class.java)
    private val service = FestivalDetailService(
        festivalRepository,
        festivalEditorialRepository,
        festivalHighlightRepository,
        FestivalDetailProperties(endingSoonDays = 7),
    )

    @Test
    fun `축제가 없으면 찾을 수 없음 예외다`() {
        `when`(festivalRepository.findById(FESTIVAL_ID)).thenReturn(Optional.empty<Festival>())

        assertThatThrownBy { service.detail(FESTIVAL_ID, STARTS_ON) }
            .isInstanceOf(FestivalNotFoundException::class.java)
    }

    @Test
    fun `발행된 에디토리얼이 없으면 기본 정보만 채우고 볼거리는 조회하지 않는다`() {
        stubFestival(festival(FESTIVAL_ID, STARTS_ON, ENDS_ON))
        `when`(
            festivalEditorialRepository.findByFestivalIdAndStatus(
                FESTIVAL_ID,
                FestivalEditorialStatus.PUBLISHED,
            ),
        ).thenReturn(null)

        val response = service.detail(FESTIVAL_ID, STARTS_ON)

        assertThat(response.festivalId).isEqualTo(FESTIVAL_ID)
        assertThat(response.name).isEqualTo("태백 해바라기축제")
        assertThat(response.venue).isEqualTo("구와우마을")
        assertThat(response.roadAddress).isEqualTo("강원특별자치도 태백시 구와우길 38-20")
        assertThat(response.editorial).isNull()
        verify(festivalHighlightRepository, never()).findByFestivalEditorialIdOrderBySortOrderAsc(anyLong())
    }

    @Test
    fun `DRAFT 에디토리얼은 상세에 노출하지 않는다`() {
        stubFestival(festival(FESTIVAL_ID, STARTS_ON, ENDS_ON))
        `when`(
            festivalEditorialRepository.findByFestivalIdAndStatus(
                FESTIVAL_ID,
                FestivalEditorialStatus.PUBLISHED,
            ),
        ).thenReturn(null)

        val response = service.detail(FESTIVAL_ID, STARTS_ON)

        assertThat(response.editorial).isNull()
        verify(festivalEditorialRepository).findByFestivalIdAndStatus(
            FESTIVAL_ID,
            FestivalEditorialStatus.PUBLISHED,
        )
        verify(festivalHighlightRepository, never()).findByFestivalEditorialIdOrderBySortOrderAsc(anyLong())
    }

    @Test
    fun `상태 뱃지는 시작일과 종료일 경계에서 규칙대로 판정한다`() {
        stubFestival(festival(FESTIVAL_ID, STARTS_ON, ENDS_ON))
        stubNoEditorial()

        val upcoming = service.detail(FESTIVAL_ID, STARTS_ON.minusDays(1))
        val startingToday = service.detail(FESTIVAL_ID, STARTS_ON)
        val endingSoon = service.detail(FESTIVAL_ID, ENDS_ON.minusDays(7))
        val ended = service.detail(FESTIVAL_ID, ENDS_ON.plusDays(1))

        assertThat(upcoming.phase).isEqualTo(FestivalPhase.UPCOMING)
        assertThat(upcoming.dDay).isEqualTo(1L)
        assertThat(upcoming.endsInDays).isNull()
        assertThat(startingToday.phase).isEqualTo(FestivalPhase.ONGOING)
        assertThat(startingToday.endsInDays).isEqualTo(30L)
        assertThat(endingSoon.phase).isEqualTo(FestivalPhase.ENDING_SOON)
        assertThat(endingSoon.endsInDays).isEqualTo(7L)
        assertThat(ended.phase).isEqualTo(FestivalPhase.ENDED)
        assertThat(ended.dDay).isNull()
        assertThat(ended.endsInDays).isNull()
    }

    @Test
    fun `종료일이 없으면 시작일을 종료일로 본다`() {
        stubFestival(festival(FESTIVAL_ID, STARTS_ON, endsOn = null))
        stubNoEditorial()

        val response = service.detail(FESTIVAL_ID, STARTS_ON)

        assertThat(response.phase).isEqualTo(FestivalPhase.ENDING_SOON)
        assertThat(response.endsInDays).isEqualTo(0L)
        assertThat(response.durationDays).isEqualTo(1)
    }

    @Test
    fun `기간 일수는 시작일과 종료일을 모두 포함한다`() {
        stubFestival(festival(FESTIVAL_ID, STARTS_ON, ENDS_ON))
        stubNoEditorial()

        val response = service.detail(FESTIVAL_ID, STARTS_ON)

        assertThat(response.durationDays).isEqualTo(31)
    }

    @Test
    fun `시작일을 판정할 수 없으면 날짜 파생 필드가 모두 null이다`() {
        stubFestival(festival(FESTIVAL_ID, startsOn = null, endsOn = ENDS_ON))
        stubNoEditorial()

        val response = service.detail(FESTIVAL_ID, STARTS_ON)

        assertThat(response.phase).isNull()
        assertThat(response.durationDays).isNull()
        assertThat(response.dDay).isNull()
        assertThat(response.endsInDays).isNull()
    }

    @Test
    fun `주요 볼거리는 정렬 파인더가 반환한 오름차순을 보존한다`() {
        stubFestival(festival(FESTIVAL_ID, STARTS_ON, ENDS_ON))
        val editorial = editorial(EDITORIAL_ID, FestivalEditorialStatus.PUBLISHED)
        `when`(
            festivalEditorialRepository.findByFestivalIdAndStatus(
                FESTIVAL_ID,
                FestivalEditorialStatus.PUBLISHED,
            ),
        ).thenReturn(editorial)
        `when`(festivalHighlightRepository.findByFestivalEditorialIdOrderBySortOrderAsc(EDITORIAL_ID))
            .thenReturn(
                listOf(
                    highlight(1, "꽃밭 트레킹 코스"),
                    highlight(2, "공연 · 체험 프로그램"),
                    highlight(3, "고산 피서 · 야간 조명"),
                ),
            )

        val response = service.detail(FESTIVAL_ID, STARTS_ON)
        val responseEditorial = requireNotNull(response.editorial)

        assertThat(responseEditorial.highlights.map { it.sortOrder }).containsExactly(1, 2, 3)
        assertThat(responseEditorial.highlights.map { it.title }).containsExactly(
            "꽃밭 트레킹 코스",
            "공연 · 체험 프로그램",
            "고산 피서 · 야간 조명",
        )
    }

    private fun stubFestival(festival: Festival) {
        `when`(festivalRepository.findById(FESTIVAL_ID)).thenReturn(Optional.of(festival))
    }

    private fun stubNoEditorial() {
        `when`(
            festivalEditorialRepository.findByFestivalIdAndStatus(
                FESTIVAL_ID,
                FestivalEditorialStatus.PUBLISHED,
            ),
        ).thenReturn(null)
    }

    private fun festival(
        id: Long,
        startsOn: LocalDate?,
        endsOn: LocalDate?,
    ): Festival {
        val festival = Festival(
            name = "태백 해바라기축제",
            venue = "구와우마을",
            startDate = "20260718",
            endDate = "20260817",
            startsOn = startsOn,
            endsOn = endsOn,
            homepageUrl = "https://example.com/festival",
            roadAddress = "강원특별자치도 태백시 구와우길 38-20",
            latitude = 37.1642,
            longitude = 128.9867,
        )
        ReflectionTestUtils.setField(festival, "id", id)
        return festival
    }

    private fun editorial(id: Long, status: FestivalEditorialStatus): FestivalEditorial {
        val editorial = FestivalEditorial(
            festivalId = FESTIVAL_ID,
            hook = "국내 최대 규모의 해바라기 축제예요.",
            heroImageUrl = "https://img/hero.jpg",
            status = status,
        )
        ReflectionTestUtils.setField(editorial, "id", id)
        return editorial
    }

    private fun highlight(sortOrder: Int, title: String): FestivalHighlight =
        FestivalHighlight(
            festivalEditorialId = EDITORIAL_ID,
            sortOrder = sortOrder,
            title = title,
            body = "볼거리 설명",
        )

    companion object {
        private const val FESTIVAL_ID = 101L
        private const val EDITORIAL_ID = 201L
        private val STARTS_ON = LocalDate.of(2026, 7, 18)
        private val ENDS_ON = LocalDate.of(2026, 8, 17)
    }
}
