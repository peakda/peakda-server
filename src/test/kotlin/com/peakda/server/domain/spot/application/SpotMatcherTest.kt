package com.peakda.server.domain.spot.application

import com.peakda.server.domain.attraction.application.AttractionEligibilityProperties
import com.peakda.server.domain.attraction.entity.Attraction
import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.spot.repository.SpotRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails

class SpotMatcherTest {
    private var candidates: List<Attraction> = emptyList()
    private val attractionRepository = mock(AttractionRepository::class.java) { invocation ->
        if (invocation.method.name == "findVisibleInBoundingBoxByContentTypes") candidates else null
    }
    private val matcher = SpotMatcher(
        mock(SpotRepository::class.java),
        attractionRepository,
        SpotMatcherProperties(radiusMeters = 50.0),
        AttractionEligibilityProperties(setOf("12")),
    )

    @Test
    fun `근처 명소 후보는 서비스 대상 유형으로만 조회한다`() {
        candidates = listOf(Attraction(tourApiContentId = "1", contentTypeCode = "12", title = "경포대", latitude = 37.7950, longitude = 128.8960))

        val result = matcher.match(37.7950, 128.8960, kakaoPlaceId = null)

        assertThat(result).isInstanceOf(SpotMatcher.MatchResult.NearbyAttraction::class.java)
        val lookup = mockingDetails(attractionRepository).invocations.single()
        assertThat(lookup.method.name).isEqualTo("findVisibleInBoundingBoxByContentTypes")
        assertThat(lookup.arguments[0]).isEqualTo(setOf("12"))
    }

    @Test
    fun `대상 유형 후보가 없으면 NoMatch 를 반환한다`() {
        val result = matcher.match(37.7950, 128.8960, kakaoPlaceId = null)

        assertThat(result).isEqualTo(SpotMatcher.MatchResult.NoMatch)
    }
}
