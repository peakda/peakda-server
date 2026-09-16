package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.spot.entity.SpotRecord
import com.peakda.server.domain.spot.repository.PlantRepository
import com.peakda.server.domain.spot.repository.SpotRecordPlantRepository
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * 동네형(LOCAL) Spot 의 카테고리별 현재 개화 상태를 최근 게시 기록에서 산출한다 (결정 D 변환).
 *
 * 명소형과 달리 동네형은 추정기가 없어 사용자 기록이 유일한 신호다. 그래서 카테고리마다
 * **가장 최근 기록 한 건만** 채택하고, 그 기록이 LATE(= [BloomStatus.ENDED]) 면 슬롯을 만들지 않는다.
 * ENDED 를 건너뛰고 더 오래된 기록으로 되돌아가면 이미 진 꽃이 계속 절정으로 남는다.
 *
 * 최신 판정은 방문일 → 작성 시각 → id 순으로 내림차순이다. 방문일만 비교하면 같은 날짜로 올린 기록끼리
 * 순서가 정해지지 않아, 어느 기록이 상태를 결정할지 조회 순서에 따라 달라진다.
 *
 * 지도 핀·핀 프리뷰·검색 뱃지가 같은 규칙을 쓰도록 한 곳에 모은다.
 */
@Component
class LocalSpotBloomResolver(
    private val spotRecordPlantRepository: SpotRecordPlantRepository,
    private val plantRepository: PlantRepository,
) {

    /**
     * [records] (동네형 스팟들의 게시 기록)를 스팟별 신호 목록으로 환산한다.
     * 목록은 신호를 결정한 기록이 최근일수록 앞이고, 같은 기록 안에서는 카테고리 선언 순이다.
     * 신호가 하나도 남지 않은 스팟은 결과에서 빠진다.
     */
    fun resolve(records: List<SpotRecord>): Map<Long, List<LocalBloomSignal>> {
        if (records.isEmpty()) return emptyMap()
        val categoriesByRecord = categoriesByRecord(records)
        if (categoriesByRecord.isEmpty()) return emptyMap()

        val latestBySpotCategory = linkedMapOf<Long, LinkedHashMap<BloomCategory, SpotRecord>>()
        records
            .filter { it.bloomStage != null }
            .sortedWith(RECENT_FIRST)
            .forEach { record ->
                val recordId = record.id ?: return@forEach
                val categories = categoriesByRecord[recordId].orEmpty()
                if (categories.isEmpty()) return@forEach
                val byCategory = latestBySpotCategory.getOrPut(record.spotId) { linkedMapOf() }
                categories.sortedBy { it.ordinal }.forEach { byCategory.putIfAbsent(it, record) }
            }

        return latestBySpotCategory
            .mapValues { (_, byCategory) ->
                byCategory.mapNotNull { (category, record) ->
                    val status = BloomStageStatusMapper.toStatus(requireNotNull(record.bloomStage))
                    if (status == BloomStatus.ENDED) null else LocalBloomSignal(category, status)
                }
            }
            .filterValues { it.isNotEmpty() }
    }

    /** 각 기록 id 의 꽃 카테고리 집합 (식물의 bloomCategory 브릿지 경유). */
    private fun categoriesByRecord(records: List<SpotRecord>): Map<Long, Set<BloomCategory>> {
        val recordIds = records.mapNotNull { it.id }
        if (recordIds.isEmpty()) return emptyMap()
        val joins = spotRecordPlantRepository.findByIdSpotRecordIdIn(recordIds)
        if (joins.isEmpty()) return emptyMap()
        val categoryByPlant = plantRepository.findAllById(joins.map { it.plantId }.toSet())
            .mapNotNull { plant -> plant.bloomCategory?.let { requireNotNull(plant.id) to it } }
            .toMap()
        return joins
            .mapNotNull { join -> categoryByPlant[join.plantId]?.let { join.spotRecordId to it } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, categories) -> categories.toSet() }
    }

    companion object {
        private val SpotRecord.observedDate: LocalDate
            get() = visitedDate ?: createdAt.atZone(ZoneOffset.UTC).toLocalDate()

        private val RECENT_FIRST = compareByDescending<SpotRecord> { it.observedDate }
            .thenByDescending { it.createdAt }
            .thenByDescending { it.id ?: 0L }
    }
}
