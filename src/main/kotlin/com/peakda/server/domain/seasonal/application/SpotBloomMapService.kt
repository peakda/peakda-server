package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.attraction.entity.Attraction
import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.seasonal.entity.Region
import com.peakda.server.domain.seasonal.entity.SeasonalBloomEstimate
import com.peakda.server.domain.seasonal.presentation.response.BloomMapResponse
import com.peakda.server.domain.seasonal.presentation.response.BloomMapResponse.BloomMapItem
import com.peakda.server.domain.seasonal.presentation.response.BloomMapResponse.BloomMapPin
import com.peakda.server.domain.seasonal.presentation.response.BloomMapResponse.BloomSlot
import com.peakda.server.domain.seasonal.repository.SeasonalBloomEstimateRepository
import com.peakda.server.domain.spot.entity.Spot
import com.peakda.server.domain.spot.entity.SpotRecord
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.entity.SpotType
import com.peakda.server.domain.spot.repository.PlantRepository
import com.peakda.server.domain.spot.repository.SpotRecordPlantRepository
import com.peakda.server.domain.spot.repository.SpotRecordRepository
import com.peakda.server.domain.spot.repository.SpotRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * 지도 영역(bbox) 내 Spot 핀별 개화 상태를 조립한다 (결정 A — Spot 중심 지도).
 *
 * - 명소형 핀: 좌표 보유 visible 명소를 [SeasonalBloomEstimate] 최신 산출일 기준으로 상속한다.
 *   이미 materialize 된 Spot 행이 있으면 spotId 를 채운다.
 * - 동네형 핀: 사용자 생성 LOCAL Spot 을 최근 게시 [SpotRecord] 신호로 산출한다 (결정 D 변환).
 * - 방문예정일 [date] 가 주어지면 명소형 슬롯을 절정 구간 기준으로 재계산한다 (결정 C MVP 산식).
 *   동네형은 관측값이라 미래 투영이 불가하므로 최근 관측 상태를 유지한다.
 *
 * 핀=3단계(PREPARING/STARTED/PEAK)만 노출하고 ENDED 슬롯은 제외한다.
 */
@Service
class SpotBloomMapService(
    private val attractionRepository: AttractionRepository,
    private val seasonalBloomEstimateRepository: SeasonalBloomEstimateRepository,
    private val spotRepository: SpotRepository,
    private val spotRecordRepository: SpotRecordRepository,
    private val spotRecordPlantRepository: SpotRecordPlantRepository,
    private val plantRepository: PlantRepository,
) {
    @Transactional(readOnly = true)
    fun map(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double,
        category: BloomCategory?,
        date: LocalDate?,
    ): BloomMapResponse = map(
        minLat = minLat,
        maxLat = maxLat,
        minLng = minLng,
        maxLng = maxLng,
        categories = category?.let(::listOf),
        status = null,
        region = null,
        date = date,
    )

    @Transactional(readOnly = true)
    fun map(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double,
        categories: List<BloomCategory>?,
        status: BloomStatus?,
        region: Region?,
        date: LocalDate?,
    ): BloomMapResponse {
        val baseDate = seasonalBloomEstimateRepository.findLatestBaseDate()
        val pins = buildAttractionPins(minLat, maxLat, minLng, maxLng, categories, status, region, date, baseDate) +
            buildLocalPins(minLat, maxLat, minLng, maxLng, categories, status, region)
        // 하위호환: 명소형 핀만 옛 구조(attractions)로도 함께 제공한다.
        val legacyAttractions = pins.filter { it.type == SpotType.ATTRACTION }.map { it.toLegacyItem() }
        return BloomMapResponse(baseDate = baseDate, count = pins.size, pins = pins, attractions = legacyAttractions)
    }

    private fun buildAttractionPins(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double,
        categories: List<BloomCategory>?,
        status: BloomStatus?,
        region: Region?,
        date: LocalDate?,
        baseDate: LocalDate?,
    ): List<BloomMapPin> {
        if (baseDate == null) return emptyList()
        val attractionsById = attractionRepository
            .findVisibleInBoundingBox(minLat = minLat, maxLat = maxLat, minLng = minLng, maxLng = maxLng)
            .filter { attraction -> region == null || Region.ofAreaCode(attraction.areaCode.orEmpty()) == region }
            .associateBy { requireNotNull(it.id) }
        if (attractionsById.isEmpty()) return emptyList()

        val ids = attractionsById.keys.toList()
        val selectedCategories = categories.orEmpty().distinct()
        val estimates = when {
            selectedCategories.isEmpty() -> seasonalBloomEstimateRepository.findByBaseDateAndAttractionIdIn(baseDate, ids)
            selectedCategories.size == 1 -> seasonalBloomEstimateRepository
                .findByBaseDateAndAttractionIdInAndBloomCategory(baseDate, ids, selectedCategories.first())
            else -> seasonalBloomEstimateRepository
                .findByBaseDateAndAttractionIdInAndBloomCategoryIn(baseDate, ids, selectedCategories)
        }
        val spotIdByAttraction = spotRepository
            .findByTypeAndAttractionIdIn(SpotType.ATTRACTION, ids)
            .mapNotNull { spot -> spot.attractionId?.let { it to requireNotNull(spot.id) } }
            .toMap()

        return estimates
            .groupBy { it.attractionId }
            .mapNotNull { (attractionId, rows) ->
                val attraction = attractionsById[attractionId] ?: return@mapNotNull null
                val slots = rows.mapNotNull { it.toSlot(date) }
                    .filter { status == null || it.status == status }
                if (slots.isEmpty()) return@mapNotNull null
                attraction.toPin(spotIdByAttraction[attractionId], slots)
            }
    }

    private fun buildLocalPins(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double,
        categories: List<BloomCategory>?,
        status: BloomStatus?,
        region: Region?,
    ): List<BloomMapPin> {
        val spots = spotRepository.findVisibleInBoundingBox(SpotType.LOCAL, minLat, maxLat, minLng, maxLng)
            .filter { spot -> region == null || Region.ofAddress(spot.address) == region }
        if (spots.isEmpty()) return emptyList()

        val records = spotRecordRepository
            .findBySpotIdInAndStatus(spots.mapNotNull { it.id }, SpotRecordStatus.PUBLISHED)
        if (records.isEmpty()) return emptyList()

        val categoriesByRecord = categoriesByRecord(records)
        val recordsBySpot = records.groupBy { it.spotId }

        return spots.mapNotNull { spot ->
            val spotId = spot.id ?: return@mapNotNull null
            val slots = localSlots(recordsBySpot[spotId].orEmpty(), categoriesByRecord, categories, status)
            if (slots.isEmpty()) return@mapNotNull null
            spot.toPin(slots)
        }
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

    /** 한 동네형 Spot 의 카테고리별 슬롯 — 최근 관측 우선, ENDED(지는 중) 제외. */
    private fun localSlots(
        spotRecords: List<SpotRecord>,
        categoriesByRecord: Map<Long, Set<BloomCategory>>,
        categoryFilters: List<BloomCategory>?,
        statusFilter: BloomStatus?,
    ): List<BloomSlot> {
        val slotByCategory = linkedMapOf<BloomCategory, BloomSlot>()
        val recent = spotRecords.sortedWith(
            compareByDescending<SpotRecord> { it.visitedDate ?: LocalDate.MIN }.thenByDescending { it.createdAt },
        )
        for (record in recent) {
            val stage = record.bloomStage ?: continue
            val status = BloomStageStatusMapper.toStatus(stage)
            if (status == BloomStatus.ENDED) continue
            val recordId = record.id ?: continue
            val categories = categoriesByRecord[recordId].orEmpty()
                .filter { categoryFilters.isNullOrEmpty() || it in categoryFilters }
            for (category in categories) {
                slotByCategory.getOrPut(category) {
                    BloomSlot(category, category.displayName, status, LOCAL_RECORD_CONFIDENCE)
                }
            }
        }
        return slotByCategory.values.filter { statusFilter == null || it.status == statusFilter }
    }

    /**
     * 절정 구간 기준 슬롯 변환. [date] 가 주어지면 그날 상태를 재계산하고, 없으면 저장된 산출 상태를 쓴다.
     * ENDED 는 핀에서 제외하므로 null 을 반환한다.
     */
    private fun SeasonalBloomEstimate.toSlot(date: LocalDate?): BloomSlot? {
        val effectiveStatus = if (date == null) status else statusOn(date)
        if (effectiveStatus == BloomStatus.ENDED) return null
        return BloomSlot(bloomCategory, bloomCategory.displayName, effectiveStatus, confidence)
    }

    /** 결정 C MVP 산식 — D 가 절정구간이면 PEAK, 직전 [STARTED_WINDOW_DAYS] 일이면 STARTED, 종료 후면 ENDED, 그 외 PREPARING. */
    private fun SeasonalBloomEstimate.statusOn(date: LocalDate): BloomStatus {
        val start = peakStartDate ?: return status
        val end = peakEndDate ?: start
        return when {
            !date.isBefore(start) && !date.isAfter(end) -> BloomStatus.PEAK
            !date.isBefore(start.minusDays(STARTED_WINDOW_DAYS)) && date.isBefore(start) -> BloomStatus.STARTED
            date.isAfter(end) -> BloomStatus.ENDED
            else -> BloomStatus.PREPARING
        }
    }

    private fun Attraction.toPin(spotId: Long?, slots: List<BloomSlot>) = BloomMapPin(
        spotId = spotId,
        attractionId = requireNotNull(id),
        type = SpotType.ATTRACTION,
        name = title,
        latitude = latitude,
        longitude = longitude,
        blooms = slots,
    )

    private fun Spot.toPin(slots: List<BloomSlot>) = BloomMapPin(
        spotId = requireNotNull(id),
        attractionId = null,
        type = SpotType.LOCAL,
        name = name,
        latitude = latitude,
        longitude = longitude,
        blooms = slots,
    )

    private fun BloomMapPin.toLegacyItem() = BloomMapItem(
        attractionId = requireNotNull(attractionId),
        title = name,
        latitude = latitude,
        longitude = longitude,
        blooms = blooms,
    )

    companion object {
        private const val STARTED_WINDOW_DAYS = 7L
        private const val LOCAL_RECORD_CONFIDENCE = 0.5
    }
}
