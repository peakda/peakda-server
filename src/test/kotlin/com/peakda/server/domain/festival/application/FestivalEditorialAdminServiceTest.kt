package com.peakda.server.domain.festival.application

import com.peakda.server.domain.festival.entity.FestivalEditorial
import com.peakda.server.domain.festival.entity.FestivalEditorialStatus
import com.peakda.server.domain.festival.entity.FestivalHighlight
import com.peakda.server.domain.festival.exception.FestivalEditorialNotFoundException
import com.peakda.server.domain.festival.exception.FestivalNotFoundException
import com.peakda.server.domain.festival.repository.FestivalEditorialRepository
import com.peakda.server.domain.festival.repository.FestivalHighlightRepository
import com.peakda.server.domain.festival.repository.FestivalRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.test.util.ReflectionTestUtils
import java.time.Instant

class FestivalEditorialAdminServiceTest {

    private val festivalRepository = mock(FestivalRepository::class.java)
    private val festivalEditorialRepository = mock(FestivalEditorialRepository::class.java)
    private val festivalHighlightRepository = mock(FestivalHighlightRepository::class.java)
    private val service = FestivalEditorialAdminService(
        festivalRepository,
        festivalEditorialRepository,
        festivalHighlightRepository,
    )

    @Test
    fun `같은 축제로 두 번 저장하면 새 행 없이 갱신하고 볼거리를 전량 교체한다`() {
        val persisted = editorial(EDITORIAL_ID, FestivalEditorialStatus.DRAFT, hook = "첫 훅")
        `when`(festivalRepository.existsById(FESTIVAL_ID)).thenReturn(true)
        `when`(festivalEditorialRepository.findByFestivalId(FESTIVAL_ID)).thenReturn(null, persisted)
        `when`(festivalEditorialRepository.save(anyEditorial())).thenReturn(persisted)

        val firstId = service.upsert(FESTIVAL_ID, command(hook = "첫 훅"))
        val secondId = service.upsert(FESTIVAL_ID, command(hook = "수정 훅"))

        assertThat(firstId).isEqualTo(EDITORIAL_ID)
        assertThat(secondId).isEqualTo(EDITORIAL_ID)
        assertThat(persisted.hook).isEqualTo("수정 훅")
        verify(festivalEditorialRepository, times(1)).save(anyEditorial())
        verify(festivalHighlightRepository, times(2)).deleteByFestivalEditorialId(EDITORIAL_ID)
        verify(festivalHighlightRepository, times(2)).saveAll(anyHighlightIterable())
    }

    @Test
    fun `볼거리 순서는 요청 배열 기준으로 1부터 서버가 부여한다`() {
        val persisted = editorial(EDITORIAL_ID, FestivalEditorialStatus.DRAFT)
        `when`(festivalRepository.existsById(FESTIVAL_ID)).thenReturn(true)
        `when`(festivalEditorialRepository.findByFestivalId(FESTIVAL_ID)).thenReturn(persisted)
        val highlights = listOf(
            highlightCommand("셋째"),
            highlightCommand("첫째"),
            highlightCommand("둘째"),
        )

        service.upsert(FESTIVAL_ID, command(highlights = highlights))

        val captor = highlightIterableCaptor()
        verify(festivalHighlightRepository).saveAll(captureHighlights(captor))
        val saved = captor.value.toList()
        assertThat(saved.map { it.sortOrder }).containsExactly(1, 2, 3)
        assertThat(saved.map { it.title }).containsExactly("셋째", "첫째", "둘째")
    }

    @Test
    fun `DRAFT에서 PUBLISHED로 바뀌면 발행 시각을 채운다`() {
        val persisted = editorial(EDITORIAL_ID, FestivalEditorialStatus.DRAFT)
        stubExisting(persisted)

        service.upsert(FESTIVAL_ID, command(status = FestivalEditorialStatus.PUBLISHED))

        assertThat(persisted.publishedAt).isNotNull()
    }

    @Test
    fun `이미 발행된 에디토리얼은 기존 발행 시각을 유지한다`() {
        val publishedAt = Instant.parse("2026-07-25T00:00:00Z")
        val persisted = editorial(
            EDITORIAL_ID,
            FestivalEditorialStatus.PUBLISHED,
            publishedAt = publishedAt,
        )
        stubExisting(persisted)

        service.upsert(FESTIVAL_ID, command(status = FestivalEditorialStatus.PUBLISHED))

        assertThat(persisted.publishedAt).isEqualTo(publishedAt)
    }

    @Test
    fun `PUBLISHED에서 DRAFT로 내리면 발행 시각을 비운다`() {
        val persisted = editorial(
            EDITORIAL_ID,
            FestivalEditorialStatus.PUBLISHED,
            publishedAt = Instant.parse("2026-07-25T00:00:00Z"),
        )
        stubExisting(persisted)

        service.upsert(FESTIVAL_ID, command(status = FestivalEditorialStatus.DRAFT))

        assertThat(persisted.publishedAt).isNull()
    }

    @Test
    fun `없는 축제에 에디토리얼을 저장하면 찾을 수 없음 예외다`() {
        `when`(festivalRepository.existsById(FESTIVAL_ID)).thenReturn(false)

        assertThatThrownBy { service.upsert(FESTIVAL_ID, command()) }
            .isInstanceOf(FestivalNotFoundException::class.java)
    }

    @Test
    fun `없는 에디토리얼을 삭제하면 찾을 수 없음 예외다`() {
        `when`(festivalEditorialRepository.findByFestivalId(FESTIVAL_ID)).thenReturn(null)

        assertThatThrownBy { service.delete(FESTIVAL_ID) }
            .isInstanceOf(FestivalEditorialNotFoundException::class.java)
    }

    @Test
    fun `삭제는 볼거리를 먼저 지운 뒤 에디토리얼을 지운다`() {
        val persisted = editorial(EDITORIAL_ID, FestivalEditorialStatus.DRAFT)
        `when`(festivalEditorialRepository.findByFestivalId(FESTIVAL_ID)).thenReturn(persisted)

        service.delete(FESTIVAL_ID)

        val order = inOrder(festivalHighlightRepository, festivalEditorialRepository)
        order.verify(festivalHighlightRepository).deleteByFestivalEditorialId(EDITORIAL_ID)
        order.verify(festivalEditorialRepository).delete(persisted)
    }

    private fun stubExisting(editorial: FestivalEditorial) {
        `when`(festivalRepository.existsById(FESTIVAL_ID)).thenReturn(true)
        `when`(festivalEditorialRepository.findByFestivalId(FESTIVAL_ID)).thenReturn(editorial)
    }

    private fun command(
        hook: String = "국내 최대 규모의 해바라기 축제예요.",
        status: FestivalEditorialStatus = FestivalEditorialStatus.DRAFT,
        highlights: List<UpsertFestivalHighlightCommand> = listOf(highlightCommand("꽃밭 트레킹 코스")),
    ): UpsertFestivalEditorialCommand = UpsertFestivalEditorialCommand(
        hook = hook,
        periodNote = "2026년 일정은 출발 전 재확인",
        placeNote = "해발 800m · 서울에서 약 2시간 40분",
        admissionFee = "성인 7,000원 · 청소년 5,000원",
        admissionFeeNote = "어린이(초등 이하) 3,000원",
        operatingHours = "09:00 ~ 18:00",
        operatingHoursNote = "입장 마감 17:30",
        caution = "고산지대 날씨 급변 · 겉옷 필수",
        cautionNote = "오전 방문 권장 (오후 역광)",
        directionsTransit = "서울 → KTX 태백역 (약 2시간) → 택시 15분",
        directionsCar = "서울 → 영동고속도로 → 태백",
        heroImageUrl = "https://img/hero.jpg",
        status = status,
        highlights = highlights,
    )

    private fun highlightCommand(title: String): UpsertFestivalHighlightCommand =
        UpsertFestivalHighlightCommand(
            title = title,
            body = "볼거리 설명",
        )

    private fun editorial(
        id: Long,
        status: FestivalEditorialStatus,
        hook: String = "훅",
        publishedAt: Instant? = null,
    ): FestivalEditorial {
        val editorial = FestivalEditorial(
            festivalId = FESTIVAL_ID,
            hook = hook,
            status = status,
            publishedAt = publishedAt,
        )
        ReflectionTestUtils.setField(editorial, "id", id)
        return editorial
    }

    @Suppress("UNCHECKED_CAST")
    private fun highlightIterableCaptor(): ArgumentCaptor<Iterable<FestivalHighlight>> =
        ArgumentCaptor.forClass(Iterable::class.java) as ArgumentCaptor<Iterable<FestivalHighlight>>

    private fun captureHighlights(
        captor: ArgumentCaptor<Iterable<FestivalHighlight>>,
    ): Iterable<FestivalHighlight> = captor.capture() ?: emptyList()

    private fun anyEditorial(): FestivalEditorial =
        any(FestivalEditorial::class.java) ?: editorial(Long.MIN_VALUE, FestivalEditorialStatus.DRAFT)

    @Suppress("UNCHECKED_CAST")
    private fun anyHighlightIterable(): Iterable<FestivalHighlight> =
        any(Iterable::class.java) as? Iterable<FestivalHighlight> ?: emptyList()

    companion object {
        private const val FESTIVAL_ID = 101L
        private const val EDITORIAL_ID = 201L
    }
}
