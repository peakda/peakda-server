package com.peakda.server.domain.festival.application

import com.peakda.server.domain.festival.entity.Festival
import com.peakda.server.domain.festival.repository.FestivalRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyDouble
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate

class NearbyFestivalServiceTest {
    private val repository = mock(FestivalRepository::class.java)
    private val service = NearbyFestivalService(repository, FestivalDetailProperties(endingSoonDays = 3))
    private val today = LocalDate.of(2026, 4, 1)

    @Test
    fun `반경 밖과 끝난 축제는 빼고 진행 중을 먼저, 가까운 순으로 상한만큼 돌려준다`() {
        `when`(
            repository.findInBoundingBoxOverlapping(
                anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDate(), anyDate(),
            ),
        ).thenReturn(
            listOf(
                festival(1, "곧 열리는 축제", today.plusDays(5), today.plusDays(8), 35.1501, 128.6601),
                festival(2, "진행 중 먼 축제", today.minusDays(2), today.plusDays(10), 35.1700, 128.6600),
                festival(3, "진행 중 가까운 축제", today.minusDays(1), today.plusDays(10), 35.1510, 128.6600),
                festival(4, "반경 밖 축제", today, today.plusDays(10), 35.3000, 128.6600),
            ),
        )

        val result = service.findNearby(35.15, 128.66, today, 5_000.0, 14, 2)

        assertThat(result).extracting<String> { it.name }.containsExactly("진행 중 가까운 축제", "진행 중 먼 축제")
        assertThat(result.first().phase).isEqualTo(FestivalPhase.ONGOING)
    }

    private fun anyDate(): LocalDate = any(LocalDate::class.java) ?: today

    private fun festival(id: Long, name: String, startsOn: LocalDate, endsOn: LocalDate, lat: Double, lng: Double): Festival {
        val festival = Festival(
            name = name,
            venue = "축제장",
            startDate = startsOn.toString(),
            endDate = endsOn.toString(),
            startsOn = startsOn,
            endsOn = endsOn,
            latitude = lat,
            longitude = lng,
        )
        ReflectionTestUtils.setField(festival, "id", id)
        return festival
    }
}
