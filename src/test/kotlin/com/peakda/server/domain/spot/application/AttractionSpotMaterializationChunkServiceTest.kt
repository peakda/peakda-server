package com.peakda.server.domain.spot.application

import com.peakda.server.domain.attraction.application.AttractionEligibilityProperties
import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.spot.entity.Spot
import com.peakda.server.domain.spot.entity.SpotType
import com.peakda.server.domain.spot.repository.SpotRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.springframework.test.util.ReflectionTestUtils

class AttractionSpotMaterializationChunkServiceTest {
    private val attractionRepository = mock(AttractionRepository::class.java) { invocation ->
        if (invocation.method.name == "findVisibleIdsByIdInAndContentTypes") listOf(1L) else null
    }
    private val spotRepository = mock(SpotRepository::class.java) { invocation ->
        if (invocation.method.name == "hideByIdIn") (invocation.arguments[0] as Collection<*>).size else null
    }
    private val service = AttractionSpotMaterializationChunkService(
        mock(SpotService::class.java),
        spotRepository,
        attractionRepository,
        AttractionEligibilityProperties(setOf("12")),
    )

    @Test
    fun `대상 명소에 연결된 Spot 만 남기고 나머지는 숨긴다`() {
        val tourist = spot(id = 10L, attractionId = 1L)
        val exhibition = spot(id = 20L, attractionId = 2L)

        val hidden = service.hideIneligible(listOf(tourist, exhibition))

        assertThat(hidden).isEqualTo(1)
        val lookup = mockingDetails(attractionRepository).invocations.single()
        assertThat(lookup.arguments[0] as Collection<*>).containsExactly(1L, 2L)
        assertThat(lookup.arguments[1]).isEqualTo(setOf("12"))
        val hide = mockingDetails(spotRepository).invocations.single()
        assertThat(hide.arguments[0] as Collection<*>).containsExactly(20L)
    }

    @Test
    fun `모두 대상이면 숨김 쿼리를 실행하지 않는다`() {
        val hidden = service.hideIneligible(listOf(spot(id = 10L, attractionId = 1L)))

        assertThat(hidden).isZero()
        assertThat(mockingDetails(spotRepository).invocations).isEmpty()
    }

    private fun spot(id: Long, attractionId: Long): Spot =
        Spot(type = SpotType.ATTRACTION, attractionId = attractionId, name = "spot $id", latitude = 37.5, longitude = 127.0)
            .also { ReflectionTestUtils.setField(it, "id", id) }
}
