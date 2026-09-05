package com.peakda.server.domain.spot.application

import com.peakda.server.domain.attraction.entity.Attraction
import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.spot.entity.Spot
import com.peakda.server.domain.spot.entity.SpotType
import com.peakda.server.domain.spot.repository.SpotRepository
import org.springframework.stereotype.Component
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@Component
class SpotMatcher(
    private val spotRepository: SpotRepository,
    private val attractionRepository: AttractionRepository,
    private val properties: SpotMatcherProperties,
) {

    fun match(latitude: Double, longitude: Double, kakaoPlaceId: String?): MatchResult {
        if (!kakaoPlaceId.isNullOrBlank()) {
            spotRepository.findByTypeAndKakaoPlaceId(SpotType.LOCAL, kakaoPlaceId)?.let {
                return MatchResult.ExistingSpot(it)
            }
        }
        findNearestAttraction(latitude, longitude)?.let {
            return MatchResult.NearbyAttraction(it)
        }
        return MatchResult.NoMatch
    }

    private fun findNearestAttraction(latitude: Double, longitude: Double): Attraction? {
        val radius = properties.radiusMeters
        val latDelta = radius / METERS_PER_DEGREE_LAT
        val cosLat = max(cos(Math.toRadians(latitude)), MIN_COS_LAT)
        val lngDelta = radius / (METERS_PER_DEGREE_LAT * cosLat)
        val candidates = attractionRepository.findVisibleInBoundingBox(
            minLat = latitude - latDelta,
            maxLat = latitude + latDelta,
            minLng = longitude - lngDelta,
            maxLng = longitude + lngDelta,
        )
        return candidates
            .mapNotNull { attraction ->
                val candLat = attraction.latitude ?: return@mapNotNull null
                val candLng = attraction.longitude ?: return@mapNotNull null
                attraction to haversine(latitude, longitude, candLat, candLng)
            }
            .filter { it.second <= radius }
            .minByOrNull { it.second }
            ?.first
    }

    private fun haversine(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2.0)
        return EARTH_RADIUS_METERS * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    sealed class MatchResult {
        data class ExistingSpot(val spot: Spot) : MatchResult()
        data class NearbyAttraction(val attraction: Attraction) : MatchResult()
        data object NoMatch : MatchResult()
    }

    companion object {
        private const val METERS_PER_DEGREE_LAT = 111_320.0
        private const val EARTH_RADIUS_METERS = 6_371_000.0
        private const val MIN_COS_LAT = 0.01
    }
}
