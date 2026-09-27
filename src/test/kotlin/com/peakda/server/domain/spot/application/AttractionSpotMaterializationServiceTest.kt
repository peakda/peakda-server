package com.peakda.server.domain.spot.application

import com.peakda.server.domain.attraction.application.AttractionEligibilityProperties
import com.peakda.server.domain.attraction.entity.Attraction
import com.peakda.server.domain.attraction.repository.AttractionRepository
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
    private val chunkService = mock(AttractionSpotMaterializationChunkService::class.java) { invocation ->
        if (invocation.method.name == "materialize") AttractionSpotMaterializationChunkResult(1, 0) else null
    }
    private val service = AttractionSpotMaterializationService(
        attractionRepository,
        chunkService,
        AttractionEligibilityProperties(setOf("12")),
    )

    @Test
    fun `서비스 대상 유형의 visible 명소만 Spot 으로 만든다`() {
        val result = service.materializeVisibleAttractions()

        assertThat(result.processed).isEqualTo(1)
        val lookup = mockingDetails(attractionRepository).invocations.single()
        assertThat(lookup.method.name).isEqualTo("findByVisibleTrueAndContentTypeCodeIn")
        assertThat(lookup.arguments[0]).isEqualTo(setOf("12"))
    }
}
