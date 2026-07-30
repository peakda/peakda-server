package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomObservation
import com.peakda.server.domain.seasonal.repository.BloomObservationRepository
import com.peakda.server.infrastructure.external.kma.flower.FlowerObservationStationCatalog
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.core.io.ByteArrayResource
import java.time.LocalDate

class ObservationSnapshotServiceTest {

    private val repository = mock(BloomObservationRepository::class.java)
    private val catalog = FlowerObservationStationCatalog(
        ByteArrayResource(
            """
                obsPlace,stnId,stnName
                여의도 윤중로,108,서울
                서울 다른곳,108,서울
            """.trimIndent().toByteArray(),
        ),
    )
    private val service = ObservationSnapshotService(repository, catalog)

    @Test
    fun `카탈로그와 벚나무 관측만 지점 카테고리 스냅샷으로 전파한다`() {
        `when`(repository.findByObsYear(2026)).thenReturn(
            listOf(
                observation(treeType = "벚나무", obsPlace = "여의도 윤중로"),
                observation(treeType = "철쭉", obsPlace = "여의도 윤중로"),
                observation(treeType = "벚나무", obsPlace = "카탈로그 없음"),
            ),
        )

        val result = service.findByStationAndCategory(2026)

        assertThat(result).containsOnlyKeys("108")
        assertThat(result["108"]).containsOnlyKeys(BloomCategory.CHERRY)
    }

    @Test
    fun `같은 지점 카테고리에 관측이 여럿이면 만발일 있는 관측을 고른다`() {
        val fullBloom = LocalDate.of(2026, 4, 1)
        `when`(repository.findByObsYear(2026)).thenReturn(
            listOf(
                observation(obsPlace = "여의도 윤중로", floweringOn = LocalDate.of(2026, 3, 24), fullBloomOn = null),
                observation(obsPlace = "서울 다른곳", floweringOn = LocalDate.of(2026, 3, 27), fullBloomOn = fullBloom),
            ),
        )

        val snapshot = service.findByStationAndCategory(2026)["108"]!![BloomCategory.CHERRY]!!

        assertThat(snapshot.obsPlace).isEqualTo("서울 다른곳")
        assertThat(snapshot.fullBloomOn).isEqualTo(fullBloom)
    }

    @Test
    fun `만발일 조건이 같으면 개화일이 이른 관측을 고른다`() {
        `when`(repository.findByObsYear(2026)).thenReturn(
            listOf(
                observation(obsPlace = "여의도 윤중로", floweringOn = LocalDate.of(2026, 3, 26)),
                observation(obsPlace = "서울 다른곳", floweringOn = LocalDate.of(2026, 3, 24)),
            ),
        )

        val snapshot = service.findByStationAndCategory(2026)["108"]!![BloomCategory.CHERRY]!!

        assertThat(snapshot.obsPlace).isEqualTo("서울 다른곳")
        assertThat(snapshot.floweringOn).isEqualTo(LocalDate.of(2026, 3, 24))
    }

    private fun observation(
        treeType: String = "벚나무",
        obsPlace: String = "여의도 윤중로",
        floweringOn: LocalDate? = LocalDate.of(2026, 3, 25),
        fullBloomOn: LocalDate? = null,
    ) = BloomObservation(
        treeType = treeType,
        obsPlace = obsPlace,
        obsYear = 2026,
        floweringOn = floweringOn,
        fullBloomOn = fullBloomOn,
    )
}
