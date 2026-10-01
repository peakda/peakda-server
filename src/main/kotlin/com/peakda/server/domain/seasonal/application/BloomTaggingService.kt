package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.attraction.application.AttractionEligibilityProperties
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
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
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
 * - 신호 B([tagFestivals]): 활성 꽃축제의 장소 토큰([FestivalPlaceTokenizer])이 후보 반경 안 명소 제목과 일치하면 FESTIVAL 태그.
 *   축제 좌표는 주최 기관 주소일 수 있어 후보를 좁히는 데만 쓴다.
 * - 신호 C([tagCategories]): TourAPI 소분류가 [BloomTaggingProperties.categoryTags] 에 있으면 CATEGORY 태그.
 *   이름에 꽃 단어가 없는 국립공원·수목원 같은 명소를 유형으로 보강한다.
 *
 * 세 신호는 매 실행 태그를 다시 upsert 하므로, [deleteStaleAutoTags] 로 이번 실행에서 갱신되지 않은 자동 태그를 지운다.
 *
 * 신호 B 의 대상 유형은 [AttractionEligibilityProperties] 로 쿼리에서 제한한다. 신호 A·C 는 호출자가 대상 유형만 넘긴다.
 */
@Service
class BloomTaggingService(
    private val attractionRepository: AttractionRepository,
    private val festivalRepository: FestivalRepository,
    private val attractionBloomRepository: AttractionBloomRepository,
    private val properties: BloomTaggingProperties,
    private val eligibilityProperties: AttractionEligibilityProperties,
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

    /** 신호 B. 종료 후 [BloomTaggingProperties.festivalTagRetentionDays] 이내인 꽃축제의 장소명과 일치하는 후보 반경 안 명소에 FESTIVAL 태그를 upsert 하고 처리한 태그 수를 반환. */
    @Transactional
    fun tagFestivals(today: LocalDate): Int {
        var count = 0
        val radiusMeters = properties.festivalCandidateRadiusKm * METERS_PER_KM
        for (festival in festivalRepository.findByLatitudeIsNotNullAndLongitudeIsNotNull()) {
            if (!isWithinRetention(festival, today)) continue
            val lat = festival.latitude ?: continue
            val lng = festival.longitude ?: continue
            val category = BloomCategory.ofFestivalName(festival.name) ?: continue
            val tokens = FestivalPlaceTokenizer.tokenize(
                venue = festival.venue,
                festivalName = festival.name,
                addresses = listOf(festival.roadAddress, festival.landLotAddress),
            )
            if (tokens.isEmpty()) continue
            for (attraction in findNearbyAttractions(lat, lng, radiusMeters)) {
                val attractionId = attraction.id ?: continue
                val token = matchPlaceToken(attraction.title, tokens) ?: continue
                attractionBloomRepository.upsert(
                    AttractionBloomUpsertCommand(
                        attractionId = attractionId,
                        bloomCategory = category.name,
                        source = TagSource.FESTIVAL.name,
                        confidence = properties.festivalConfidence,
                        evidence = "festival:${festival.id},name:${festival.name},token:$token",
                    ),
                )
                count++
            }
        }
        return count
    }

    /** 신호 C. TourAPI 소분류가 설정된 코드와 같은 명소에 CATEGORY 태그를 upsert 하고 처리한 태그 수를 반환. */
    @Transactional
    fun tagCategories(attractions: List<Attraction>): Int {
        if (properties.categoryTags.isEmpty()) return 0
        var count = 0
        for (attraction in attractions) {
            val attractionId = attraction.id ?: continue
            val code = attraction.categoryMinor ?: continue
            for ((category, codes) in properties.categoryTags) {
                if (code !in codes) continue
                attractionBloomRepository.upsert(
                    AttractionBloomUpsertCommand(
                        attractionId = attractionId,
                        bloomCategory = category.name,
                        source = TagSource.CATEGORY.name,
                        confidence = properties.categoryConfidence,
                        evidence = "category:$code",
                    ),
                )
                count++
            }
        }
        return count
    }

    /**
     * [runStartedAt] 실행에서 다시 만들어지지 않은 자동 태그 중 [sources] 출처만 삭제하고 삭제 수를 반환.
     * 앱·DB 시계 차이로 방금 갱신한 태그를 지우지 않도록 [STALE_GRACE] 만큼 여유를 둔다. MANUAL·EXIF_BOOST 는 지울 수 없다.
     */
    @Transactional
    fun deleteStaleAutoTags(runStartedAt: Instant, sources: Set<TagSource>): Int {
        require(AUTO_SOURCES.containsAll(sources)) { "자동 태그 출처만 정리할 수 있다: $sources" }
        if (sources.isEmpty()) return 0
        return attractionBloomRepository.deleteBySourceInAndUpdatedAtBefore(
            sources = sources,
            before = runStartedAt.minus(STALE_GRACE),
        )
    }

    private fun matchPlaceToken(title: String, tokens: Set<String>): String? {
        val normalizedTitle = FestivalPlaceTokenizer.normalize(title)
        return tokens.firstOrNull { token ->
            normalizedTitle.contains(token) ||
                (normalizedTitle.length >= MIN_CONTAINED_TITLE_LENGTH && token.contains(normalizedTitle))
        }
    }

    private fun matchKeyword(title: String, category: BloomCategory): KeywordMatch? {
        val haystack = title.lowercase()
        if (category.keywordExclusions.any { haystack.contains(it.lowercase()) }) return null
        val hint = category.keywordHints.firstOrNull { haystack.contains(it.lowercase()) } ?: return null
        val exact = haystack.contains(category.displayName.lowercase())
        val confidence = properties.keywordBaseConfidence + if (exact) properties.keywordExactBoost else 0.0
        return KeywordMatch(confidence = minOf(confidence, 1.0), evidence = "keyword:$hint")
    }

    private fun isWithinRetention(festival: Festival, today: LocalDate): Boolean {
        val end = festival.endsOn ?: festival.startsOn ?: return false
        return !end.isBefore(today.minusDays(properties.festivalTagRetentionDays))
    }

    private fun findNearbyAttractions(lat: Double, lng: Double, radiusMeters: Double): List<Attraction> {
        val latDelta = radiusMeters / METERS_PER_DEGREE_LAT
        val cosLat = max(cos(Math.toRadians(lat)), MIN_COS_LAT)
        val lngDelta = radiusMeters / (METERS_PER_DEGREE_LAT * cosLat)
        return attractionRepository.findVisibleInBoundingBoxByContentTypes(
            contentTypeCodes = eligibilityProperties.eligibleContentTypes,
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
        private const val MIN_CONTAINED_TITLE_LENGTH = 3
        private val AUTO_SOURCES = setOf(TagSource.KEYWORD, TagSource.FESTIVAL, TagSource.CATEGORY)
        private val STALE_GRACE: Duration = Duration.ofHours(1)
    }
}
