package com.peakda.server.domain.festival.application

import com.peakda.server.domain.festival.repository.FestivalRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** 명소 주변에서 열리고 있거나 곧 열릴 축제. */
@Service
class NearbyFestivalService(
    private val festivalRepository: FestivalRepository,
    private val festivalDetailProperties: FestivalDetailProperties,
) {
    /**
     * 반경 [radiusMeters] 안에서 [today] 에 진행 중이거나 [lookaheadDays] 안에 시작하는 축제를
     * 진행 중 우선, 가까운 순으로 최대 [limit] 개.
     */
    @Transactional(readOnly = true)
    fun findNearby(
        latitude: Double,
        longitude: Double,
        today: LocalDate,
        radiusMeters: Double,
        lookaheadDays: Long,
        limit: Int,
    ): List<NearbyFestival> {
        val latDelta = Math.toDegrees(radiusMeters / EARTH_RADIUS_METERS)
        val lngDelta = latDelta / cos(Math.toRadians(latitude)).coerceAtLeast(MIN_COS)
        return festivalRepository
            .findInBoundingBoxOverlapping(
                minLat = latitude - latDelta,
                maxLat = latitude + latDelta,
                minLng = longitude - lngDelta,
                maxLng = longitude + lngDelta,
                from = today,
                to = today.plusDays(lookaheadDays),
            )
            .mapNotNull { festival ->
                val startsOn = festival.startsOn ?: return@mapNotNull null
                val lat = festival.latitude ?: return@mapNotNull null
                val lng = festival.longitude ?: return@mapNotNull null
                val distance = haversine(latitude, longitude, lat, lng).takeIf { it <= radiusMeters }
                    ?: return@mapNotNull null
                val endsOn = FestivalPhaseResolver.effectiveEndsOn(startsOn, festival.endsOn) ?: startsOn
                val phase = FestivalPhaseResolver.resolve(startsOn, endsOn, today, festivalDetailProperties.endingSoonDays)
                    ?.takeIf { it != FestivalPhase.ENDED }
                    ?: return@mapNotNull null
                NearbyFestival(requireNotNull(festival.id), festival.name, startsOn, endsOn, phase, distance)
            }
            .sortedWith(compareBy<NearbyFestival> { it.phase == FestivalPhase.UPCOMING }.thenBy { it.distanceMeters })
            .take(limit)
    }

    private fun haversine(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2) * sin(dLng / 2)
        return EARTH_RADIUS_METERS * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    companion object {
        private const val EARTH_RADIUS_METERS = 6_371_000.0
        private const val MIN_COS = 0.01
    }
}
