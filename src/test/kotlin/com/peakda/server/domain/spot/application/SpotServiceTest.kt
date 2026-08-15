package com.peakda.server.domain.spot.application

import com.peakda.server.domain.attraction.entity.Attraction
import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.spot.entity.Spot
import com.peakda.server.domain.spot.entity.SpotType
import com.peakda.server.domain.spot.repository.SpotRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.test.util.ReflectionTestUtils

class SpotServiceTest {
    private val spotRepository = mock(SpotRepository::class.java)
    private val attractionRepository = mock(AttractionRepository::class.java)
    private val service = SpotService(spotRepository, attractionRepository)

    @Test
    fun `명소형 Spot이 이미 있으면 새로 저장하지 않는다`() {
        val attraction = attraction(101L)
        val existing = spot(501L, attractionId = 101L)
        doReturn(existing).`when`(spotRepository)
            .findByTypeAndAttractionId(SpotType.ATTRACTION, 101L)

        val result = service.findOrCreateForAttraction(attraction)

        assertThat(result).isSameAs(existing)
        verify(spotRepository, never()).save(existing)
    }

    private fun attraction(id: Long): Attraction {
        val attraction = Attraction(
            tourApiContentId = "content-$id",
            title = "명소 $id",
            latitude = 37.5,
            longitude = 127.0,
        )
        ReflectionTestUtils.setField(attraction, "id", id)
        return attraction
    }

    private fun spot(id: Long, attractionId: Long): Spot {
        val spot = Spot(
            type = SpotType.ATTRACTION,
            attractionId = attractionId,
            name = "명소형 스팟 $id",
            latitude = 37.5,
            longitude = 127.0,
        )
        ReflectionTestUtils.setField(spot, "id", id)
        return spot
    }
}
