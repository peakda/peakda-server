package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.attraction.entity.Attraction
import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.festival.entity.Festival
import com.peakda.server.domain.festival.repository.FestivalRepository
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.TagSource
import com.peakda.server.domain.seasonal.repository.AttractionBloomRepository
import com.peakda.server.domain.seasonal.repository.AttractionBloomUpsertCommand
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 명소 ↔ 꽃·계절 카테고리 자동 태깅. 각 신호는 독립적으로 [AttractionBloom] 행을 만들며 `(명소,카테고리,출처)` 단위로 upsert 된다.
 *
 * - 신호 A([tagKeywords]): 명소 제목에 카테고리 [BloomCategory.keywordHints] 가 포함되면 KEYWORD 태그.
 * - 신호 B([tagFestivals]): 활성 꽃축제 이름이 [BloomCategory.festivalHints] 와 일치하면 근접 명소에 FESTIVAL 태그.
 */
@Service
class BloomTaggingService(
    private val attractionRepository: AttractionRepository,
    private val festivalRepository: FestivalRepository,
    private val attractionBloomRepository: AttractionBloomRepository,
    private val properties: BloomTaggingProperties,
) {

    /** 신호 A. 주어진 명소 묶음을 키워드 매칭해 KEYWORD 태그를 upsert 하고 처리한 태그 수를 반환. */
    @Transactional
    fun tagKeywords(attractions: List<Attraction>): Int {
        var count = 0
        for (attraction in attractions) {
            val attractionId = attraction.id ?: continue
            for (category in BloomCategory.entries) {
                val match = matchKeyword(attraction.title, category) ?: continue
                attractionBloomRepository.upsert(
                    AttractionBloomUpsertCommand(
                        attractionId = attractionId,
                        bloomCategory = category.name,
                        source = TagSource.KEYWORD.name,
                        confidence = match.confidence,
                        evidence = match.evidence,
                    ),
                )
                count++
            }
        }
        return count
    }

    /** 신호 B. 활성 축제 좌표·이름으로 근접 명소에 FESTIVAL 태그를 upsert 하고 처리한 태그 수를 반환. */
    @Transactional
    fun tagFestivals(today: LocalDate): Int {
        var count = 0
        val radiusMeters = properties.festivalProximityKm * METERS_PER_KM
        for (festival in festivalRepository.findByLatitudeIsNotNullAndLongitudeIsNotNull()) {
            if (!isActive(festival, today)) continue
            val lat = festival.latitude ?: continue
            val lng = festival.longitude ?: continue
            val category = categoryOf(festival.name) ?: continue
            for (attraction in findNearbyAttractions(lat, lng, radiusMeters)) {
                val attractionId = attraction.id ?: continue
                attractionBloomRepository.upsert(
                    AttractionBloomUpsertCommand(
                        attractionId = attractionId,
                        bloomCategory = category.name,
                        source = TagSource.FESTIVAL.name,
                        confidence = properties.festivalConfidence,
                        evidence = "festival:${festival.id},name:${festival.name}",
                    ),
                )
                count++
            }
        }
        return count
    }

    private fun matchKeyword(title: String, category: BloomCategory): KeywordMatch? {
        val haystack = title.lowercase()
        val hint = category.keywordHints.firstOrNull { haystack.contains(it.lowercase()) } ?: return null
        val exact = haystack.contains(category.displayName.lowercase())
        val confidence = properties.keywordBaseConfidence + if (exact) properties.keywordExactBoost else 0.0
        return KeywordMatch(confidence = minOf(confidence, 1.0), evidence = "keyword:$hint")
    }

    private fun categoryOf(festivalName: String): BloomCategory? {
        val haystack = festivalName.lowercase()
        return BloomCategory.entries.firstOrNull { category ->
            category.festivalHints.any { haystack.contains(it.lowercase()) }
        }
    }

    private fun isActive(festival: Festival, today: LocalDate): Boolean {
        val end = parseDate(festival.endDate) ?: parseDate(festival.startDate) ?: return false
        return !end.isBefore(today)
    }

    private fun parseDate(value: String?): LocalDate? {
        if (value.isNullOrBlank()) return null
        val digits = value.filter { it.isDigit() }
        if (digits.length != 8) return null
        return try {
            LocalDate.parse(digits, DateTimeFormatter.BASIC_ISO_DATE)
        } catch (e: DateTimeParseException) {
            null
        }
    }

    private fun findNearbyAttractions(lat: Double, lng: Double, radiusMeters: Double): List<Attraction> {
        val latDelta = radiusMeters / METERS_PER_DEGREE_LAT
        val cosLat = max(cos(Math.toRadians(lat)), MIN_COS_LAT)
        val lngDelta = radiusMeters / (METERS_PER_DEGREE_LAT * cosLat)
        return attractionRepository.findVisibleInBoundingBox(
            minLat = lat - latDelta,
            maxLat = lat + latDelta,
            minLng = lng - lngDelta,
            maxLng = lng + lngDelta,
        ).filter { attraction ->
            val aLat = attraction.latitude ?: return@filter false
            val aLng = attraction.longitude ?: return@filter false
            haversine(lat, lng, aLat, aLng) <= radiusMeters
        }
    }

    private fun haversine(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2.0)
        return EARTH_RADIUS_METERS * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private data class KeywordMatch(
        val confidence: Double,
        val evidence: String,
    )

    companion object {
        private const val METERS_PER_KM = 1_000.0
        private const val METERS_PER_DEGREE_LAT = 111_320.0
        private const val EARTH_RADIUS_METERS = 6_371_000.0
        private const val MIN_COS_LAT = 0.01
    }
}
