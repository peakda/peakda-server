package com.peakda.server.domain.spot.application

import com.peakda.server.domain.attraction.entity.Attraction
import com.peakda.server.domain.spot.entity.Spot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.times

class AttractionSpotMaterializationChunkServiceTest {
    private val spotService = mock(SpotService::class.java)
    private val service = AttractionSpotMaterializationChunkService(spotService)
    private val coordinateAttraction = attraction(1L, latitude = 37.5, longitude = 127.0)

    init {
        doReturn(
            Spot(
                type = com.peakda.server.domain.spot.entity.SpotType.ATTRACTION,
                name = "명소",
                latitude = 37.5,
                longitude = 127.0,
            ),
        ).`when`(spotService).findOrCreateForAttraction(coordinateAttraction)
    }

    @Test
    fun `좌표가 없는 visible 명소는 건너뛰고 건수를 반환한다`() {
        val result = service.materialize(
            listOf(
                coordinateAttraction,
                attraction(2L, latitude = null, longitude = 127.0),
                attraction(3L, latitude = 37.5, longitude = null),
            ),
        )

        assertThat(result.processed).isEqualTo(1)
        assertThat(result.skippedNoCoordinates).isEqualTo(2)
        verify(spotService, times(1)).findOrCreateForAttraction(coordinateAttraction)
    }

    @Test
    fun `좌표가 있는 명소는 SpotService의 멱등 경로에 위임한다`() {
        service.materialize(listOf(coordinateAttraction))

        verify(spotService).findOrCreateForAttraction(coordinateAttraction)
    }

    private fun attraction(id: Long, latitude: Double?, longitude: Double?): Attraction =
        Attraction(
            tourApiContentId = "content-$id",
            title = "명소 $id",
            latitude = latitude,
            longitude = longitude,
        )
}
