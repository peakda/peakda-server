package com.peakda.server.domain.spot.application

import com.peakda.server.domain.attraction.application.AttractionEligibilityProperties
import com.peakda.server.domain.attraction.entity.Attraction
import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.spot.entity.SpotType
import com.peakda.server.domain.spot.repository.SpotRepository
import com.peakda.server.domain.spot.repository.SpotVisibilityRow
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.springframework.data.domain.SliceImpl

class AttractionSpotMaterializationServiceTest {
    private val attraction = Attraction(tourApiContentId = "1", contentTypeCode = "12", title = "경포대", latitude = 37.795, longitude = 128.896)
    private val attractionRepository = mock(AttractionRepository::class.java) { invocation ->
        if (invocation.method.name == "findByVisibleTrueAndContentTypeCodeIn") SliceImpl(listOf(attraction)) else null
    }
    private val existingSpot = object : SpotVisibilityRow {
        override val id = 8714L
        override val attractionId = 14298L
        override val visible = true
    }
    private val spotRepository = mock(SpotRepository::class.java) { invocation ->
        if (invocation.method.name == "findByTypeAndIdGreaterThanOrderByIdAsc") {
            if (invocation.arguments[1] == 0L) listOf(existingSpot) else emptyList<SpotVisibilityRow>()
        } else {
            null
        }
    }
    private val chunkService = mock(AttractionSpotMaterializationChunkService::class.java) { invocation ->
        when (invocation.method.name) {
            "materialize" -> AttractionSpotMaterializationChunkResult(1, 0)
            "syncVisibility" -> AttractionSpotVisibilitySyncResult(hidden = (invocation.arguments[0] as List<*>).size, shown = 0)
            else -> null
        }
    }
    private val service = AttractionSpotMaterializationService(
        attractionRepository,
        chunkService,
        AttractionEligibilityProperties(setOf("12")),
        spotRepository,
    )

    @Test
    fun `서비스 대상 유형의 visible 명소만 Spot 으로 만든다`() {
        val result = service.materializeVisibleAttractions()

        assertThat(result.processed).isEqualTo(1)
        val lookup = mockingDetails(attractionRepository).invocations.single()
        assertThat(lookup.method.name).isEqualTo("findByVisibleTrueAndContentTypeCodeIn")
        assertThat(lookup.arguments[0]).isEqualTo(setOf("12"))
    }

    @Test
    fun `생성 뒤 명소형 Spot 전체를 순회해 공개 여부를 맞춘다`() {
        val result = service.materializeVisibleAttractions()

        assertThat(result.hidden).isEqualTo(1)
        val hide = mockingDetails(chunkService).invocations.single { it.method.name == "syncVisibility" }
        assertThat(hide.arguments[0]).isEqualTo(listOf(existingSpot))
        val scan = mockingDetails(spotRepository).invocations.first()
        assertThat(scan.arguments[0]).isEqualTo(SpotType.ATTRACTION)
    }
}
