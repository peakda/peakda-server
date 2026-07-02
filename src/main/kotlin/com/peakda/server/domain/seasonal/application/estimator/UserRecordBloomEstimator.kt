package com.peakda.server.domain.seasonal.application.estimator

import com.peakda.server.domain.seasonal.application.BloomEstimation
import com.peakda.server.domain.seasonal.application.BloomEstimationContext
import com.peakda.server.domain.seasonal.application.BloomStageStatusMapper
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.Estimator
import com.peakda.server.domain.spot.entity.SpotRecord
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.entity.SpotType
import com.peakda.server.domain.spot.repository.PlantRepository
import com.peakda.server.domain.spot.repository.SpotRecordPlantRepository
import com.peakda.server.domain.spot.repository.SpotRecordRepository
import com.peakda.server.domain.spot.repository.SpotRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * 신호 D — 사용자 방문기록([SpotRecord]) 기반 추정기 (결정 A-3).
 *
 * 명소형 Spot(이미 materialize 된 경우만)에 달린 최근 게시 기록 중 이 카테고리로 태깅된 것을
 * [BloomStageStatusMapper] 로 상태 환산한다. [UserRecordEstimatorProperties.maxAgeDays] 를 넘긴 기록은
 * 신호로 쓰지 않고, 그 안에서는 최신일수록(0일 경과 시 baseConfidence) 신뢰도가 선형으로 높다.
 */
@Component
class UserRecordBloomEstimator(
    private val properties: UserRecordEstimatorProperties,
    private val spotRepository: SpotRepository,
    private val spotRecordRepository: SpotRecordRepository,
    private val spotRecordPlantRepository: SpotRecordPlantRepository,
    private val plantRepository: PlantRepository,
) : BloomEstimator {

    override val estimator = Estimator.USER_RECORD

    override fun estimate(context: BloomEstimationContext): BloomEstimation? {
        if (!properties.enabled) return null
        val attractionId = context.attraction.id ?: return null
        val spotId = spotRepository.findByTypeAndAttractionId(SpotType.ATTRACTION, attractionId)?.id ?: return null

        val records = spotRecordRepository.findBySpotIdAndStatusOrderByCreatedAtDesc(
            spotId,
            SpotRecordStatus.PUBLISHED,
            PageRequest.of(0, properties.lookbackRecords),
        ).content
        if (records.isEmpty()) return null

        val categoriesByRecord = categoriesByRecord(records)
        val latest = records
            .filter { record ->
                val recordId = record.id ?: return@filter false
                record.bloomStage != null && context.category in categoriesByRecord[recordId].orEmpty()
            }
            .maxByOrNull { it.recordDate }
            ?: return null

        val ageDays = ChronoUnit.DAYS.between(latest.recordDate, context.baseDate).coerceAtLeast(0)
        if (ageDays > properties.maxAgeDays) return null

        val decayRatio = ageDays.toDouble() / properties.maxAgeDays
        val confidence = properties.baseConfidence - decayRatio * (properties.baseConfidence - properties.minConfidence)

        return BloomEstimation(
            estimator = Estimator.USER_RECORD,
            status = BloomStageStatusMapper.toStatus(requireNotNull(latest.bloomStage)),
            confidence = confidence,
            evidence = "spot_record:${latest.id},stage:${latest.bloomStage}",
        )
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

    private val SpotRecord.recordDate: LocalDate
        get() = visitedDate ?: createdAt.atZone(ZoneOffset.UTC).toLocalDate()
}
