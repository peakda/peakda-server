package com.peakda.server.domain.spot.application

import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.seasonal.application.BloomStageStatusMapper
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.seasonal.entity.SeasonalBloomEstimate
import com.peakda.server.domain.seasonal.repository.SeasonalBloomEstimateRepository
import com.peakda.server.domain.spot.entity.Spot
import com.peakda.server.domain.spot.entity.SpotRecord
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.entity.SpotType
import com.peakda.server.domain.spot.presentation.response.SpotPreviewResponse
import com.peakda.server.domain.spot.presentation.response.SpotPreviewResponse.BloomBadge
import com.peakda.server.domain.spot.presentation.response.SpotPreviewResponse.SpotPreviewItem
import com.peakda.server.domain.spot.repository.PlantRepository
import com.peakda.server.domain.spot.repository.SpotRecordPhotoRepository
import com.peakda.server.domain.spot.repository.SpotRecordPlantRepository
import com.peakda.server.domain.spot.repository.SpotRecordRepository
import com.peakda.server.domain.spot.repository.SpotRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 핀 클릭 프리뷰(SCR-011d 클러스터 리스트 / SCR-011e 단일 프리뷰) 카드 조립.
 *
 * 두 화면 모두 "지도 핀 → 스팟 카드(썸네일/단계뱃지/거리)" 형태로 동일해, spotIds 1건이면 단일 프리뷰,
 * 여러 건이면 클러스터 리스트로 프론트가 그대로 렌더링한다.
 */
@Service
class SpotPreviewService(
    private val spotRepository: SpotRepository,
    private val attractionRepository: AttractionRepository,
    private val seasonalBloomEstimateRepository: SeasonalBloomEstimateRepository,
    private val spotRecordRepository: SpotRecordRepository,
    private val spotRecordPlantRepository: SpotRecordPlantRepository,
    private val plantRepository: PlantRepository,
    private val spotRecordPhotoRepository: SpotRecordPhotoRepository,
    private val spotRecordPhotoUploader: SpotRecordPhotoUploader,
) {

    @Transactional(readOnly = true)
    fun preview(spotIds: List<Long>, category: BloomCategory?, lat: Double?, lng: Double?): SpotPreviewResponse {
        val distinctIds = spotIds.distinct()
        if (distinctIds.isEmpty()) return SpotPreviewResponse(emptyList())

        val spotsById = spotRepository.findAllById(distinctIds)
            .filter { it.visible }
            .associateBy { requireNotNull(it.id) }
        if (spotsById.isEmpty()) return SpotPreviewResponse(emptyList())

        val badgeBySpot = attractionBadges(spotsById.values, category) + localBadges(spotsById.values, category)
        val thumbnailBySpot = attractionThumbnails(spotsById.values) + localThumbnails(spotsById.values)

        val items = distinctIds.mapNotNull { spotId ->
            val spot = spotsById[spotId] ?: return@mapNotNull null
            SpotPreviewItem(
                spotId = spotId,
                type = spot.type,
                name = spot.name,
                thumbnailUrl = thumbnailBySpot[spotId],
                badge = badgeBySpot[spotId],
                distanceMeters = distance(lat, lng, spot.latitude, spot.longitude),
            )
        }
        return SpotPreviewResponse(items)
    }

    /** 명소형 스팟의 대표 개화 뱃지 — 최신 산출일 기준 ENDED 를 제외한 가장 강한(상태 우선·신뢰도) 추정 1건. */
    private fun attractionBadges(spots: Collection<Spot>, category: BloomCategory?): Map<Long, BloomBadge> {
        val attractionIdBySpot = spots
            .filter { it.type == SpotType.ATTRACTION }
            .mapNotNull { spot -> spot.attractionId?.let { requireNotNull(spot.id) to it } }
        if (attractionIdBySpot.isEmpty()) return emptyMap()

        val baseDate = seasonalBloomEstimateRepository.findLatestBaseDate() ?: return emptyMap()
        val attractionIds = attractionIdBySpot.map { it.second }
        val estimates = if (category != null) {
            seasonalBloomEstimateRepository.findByBaseDateAndAttractionIdInAndBloomCategory(baseDate, attractionIds, category)
        } else {
            seasonalBloomEstimateRepository.findByBaseDateAndAttractionIdIn(baseDate, attractionIds)
        }
        val badgeByAttraction = estimates
            .filter { it.status != BloomStatus.ENDED }
            .groupBy { it.attractionId }
            .mapValues { (_, rows) -> rows.minWith(compareBy({ statusRank(it.status) }, { -it.confidence })).toBadge() }

        return attractionIdBySpot.mapNotNull { (spotId, attractionId) ->
            badgeByAttraction[attractionId]?.let { spotId to it }
        }.toMap()
    }

    /** 동네형 스팟의 대표 개화 뱃지 — 카테고리 매칭되는 최근 게시 기록 1건을 [BloomStageStatusMapper] 로 환산. */
    private fun localBadges(spots: Collection<Spot>, category: BloomCategory?): Map<Long, BloomBadge> {
        val localSpotIds = spots.filter { it.type == SpotType.LOCAL }.mapNotNull { it.id }
        if (localSpotIds.isEmpty()) return emptyMap()

        val records = spotRecordRepository.findBySpotIdInAndStatus(localSpotIds, SpotRecordStatus.PUBLISHED)
        if (records.isEmpty()) return emptyMap()

        val categoriesByRecord = categoriesByRecord(records)
        val recent = records.sortedByDescending { it.recordDate }
        val badgeBySpot = linkedMapOf<Long, BloomBadge>()
        for (record in recent) {
            val spotId = record.spotId
            if (badgeBySpot.containsKey(spotId)) continue
            val stage = record.bloomStage ?: continue
            val status = BloomStageStatusMapper.toStatus(stage)
            if (status == BloomStatus.ENDED) continue
            val recordId = record.id ?: continue
            val matched = categoriesByRecord[recordId].orEmpty()
                .firstOrNull { category == null || it == category } ?: continue
            badgeBySpot[spotId] = BloomBadge(matched, matched.displayName, status)
        }
        return badgeBySpot
    }

    private fun attractionThumbnails(spots: Collection<Spot>): Map<Long, String> {
        val attractionIdBySpot = spots
            .filter { it.type == SpotType.ATTRACTION }
            .mapNotNull { spot -> spot.attractionId?.let { requireNotNull(spot.id) to it } }
        if (attractionIdBySpot.isEmpty()) return emptyMap()

        val imageByAttraction = attractionRepository.findAllById(attractionIdBySpot.map { it.second })
            .mapNotNull { attraction ->
                (attraction.primaryImageUrl ?: attraction.thumbnailImageUrl)?.let { requireNotNull(attraction.id) to it }
            }
            .toMap()

        return attractionIdBySpot.mapNotNull { (spotId, attractionId) ->
            imageByAttraction[attractionId]?.let { spotId to it }
        }.toMap()
    }

    /** 동네형 스팟 썸네일 — 카테고리 무관, 가장 최근 게시 기록의 대표 사진. */
    private fun localThumbnails(spots: Collection<Spot>): Map<Long, String> {
        val localSpotIds = spots.filter { it.type == SpotType.LOCAL }.mapNotNull { it.id }
        if (localSpotIds.isEmpty()) return emptyMap()

        val records = spotRecordRepository.findBySpotIdInAndStatus(localSpotIds, SpotRecordStatus.PUBLISHED)
        val latestBySpot = records.groupBy { it.spotId }.mapValues { (_, rows) -> rows.maxBy { it.recordDate } }
        val recordIds = latestBySpot.values.mapNotNull { it.id }
        val photosByRecord = spotRecordPhotoRepository.findBySpotRecordIdIn(recordIds)
            .sortedBy { it.sortOrder }
            .groupBy { it.spotRecordId }

        return latestBySpot.mapNotNull { (spotId, record) ->
            val recordId = record.id ?: return@mapNotNull null
            photosByRecord[recordId]?.firstOrNull()?.let { spotId to spotRecordPhotoUploader.presignedUrlOf(it.objectKey) }
        }.toMap()
    }

    /** 각 기록 id 의 꽃 카테고리 집합 (식물의 bloomCategory 브릿지 경유). */
    private fun categoriesByRecord(records: List<SpotRecord>): Map<Long, Set<BloomCategory>> {
        val recordIds = records.mapNotNull { it.id }
        val joins = spotRecordPlantRepository.findByIdSpotRecordIdIn(recordIds)
        val categoryByPlant = plantRepository.findAllById(joins.map { it.plantId }.toSet())
            .mapNotNull { plant -> plant.bloomCategory?.let { requireNotNull(plant.id) to it } }
            .toMap()
        return joins
            .mapNotNull { join -> categoryByPlant[join.plantId]?.let { join.spotRecordId to it } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, categories) -> categories.toSet() }
    }

    private fun SeasonalBloomEstimate.toBadge() = BloomBadge(bloomCategory, bloomCategory.displayName, status)

    private val SpotRecord.recordDate: LocalDate
        get() = visitedDate ?: createdAt.atZone(ZoneOffset.UTC).toLocalDate()

    /** Haversine 거리(m). 좌표가 없으면 null. */
    private fun distance(lat: Double?, lng: Double?, spotLat: Double, spotLng: Double): Double? {
        if (lat == null || lng == null) return null
        val dLat = Math.toRadians(spotLat - lat)
        val dLng = Math.toRadians(spotLng - lng)
        val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat)) * cos(Math.toRadians(spotLat)) * sin(dLng / 2).pow(2.0)
        return EARTH_RADIUS_METERS * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    companion object {
        private const val EARTH_RADIUS_METERS = 6_371_000.0

        private fun statusRank(status: BloomStatus): Int = when (status) {
            BloomStatus.PEAK -> 0
            BloomStatus.STARTED -> 1
            BloomStatus.PREPARING -> 2
            BloomStatus.ENDED -> 3
        }
    }
}
