package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomObservation
import com.peakda.server.domain.seasonal.repository.BloomObservationRepository
import com.peakda.server.infrastructure.external.kma.flower.FlowerObservationStationCatalog
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class ObservationSnapshotService(
    private val repository: BloomObservationRepository,
    private val catalog: FlowerObservationStationCatalog,
) {
    /**
     * `지점번호 → 카테고리 → 관측` 조회. 배치 실행당 한 번만 호출한다.
     * 카탈로그에 없는 장소(철쭉·동백 관측지, 단풍 관측 산)는 지점 권역 전파 대상이 아니므로 제외한다.
     */
    @Transactional(readOnly = true)
    fun findByStationAndCategory(year: Int): Map<String, Map<BloomCategory, ObservationSnapshot>> {
        return repository.findByObsYear(year)
            .mapNotNull { observation ->
                val category = bloomCategoryOf(observation.treeType)
                    ?.takeUnless { it in MOUNTAIN_SCOPED }
                    ?: return@mapNotNull null
                val stationId = catalog.stationByPlace[observation.obsPlace] ?: return@mapNotNull null
                Triple(stationId, category, observation)
            }
            .groupBy { it.first }
            .mapValues { (_, stationCandidates) ->
                stationCandidates
                    .groupBy { it.second }
                    .mapValues { (_, categoryCandidates) ->
                        categoryCandidates.minWithOrNull(OBSERVATION_ORDER)!!.third.toSnapshot()
                    }
            }
    }

    /**
     * 유명산 단풍 관측 `산 이름 → 관측` 조회. 배치 실행당 한 번만 호출한다.
     * 단풍은 산 단위 진행이라 지점 권역이 아니라 [MapleMountainMatcher] 로 산에 속한 명소에만 연결한다.
     */
    @Transactional(readOnly = true)
    fun findMapleByMountain(year: Int): Map<String, ObservationSnapshot> {
        return repository.findByObsYear(year)
            .filter { bloomCategoryOf(it.treeType) == BloomCategory.MAPLE }
            .associate { it.obsPlace to it.toSnapshot() }
    }

    private fun bloomCategoryOf(treeType: String): BloomCategory? = when (treeType.trim()) {
        CHERRY_TREE_TYPE -> BloomCategory.CHERRY
        MAPLE_TREE_TYPE -> BloomCategory.MAPLE
        else -> null
    }

    private fun BloomObservation.toSnapshot() = ObservationSnapshot(
        obsPlace = obsPlace,
        floweringOn = floweringOn,
        fullBloomOn = fullBloomOn,
    )

    companion object {
        private const val CHERRY_TREE_TYPE = "벚나무"
        private const val MAPLE_TREE_TYPE = "단풍"

        /** 산 단위로만 연결하는 카테고리. 지점 권역 전파에서 뺀다. */
        private val MOUNTAIN_SCOPED = setOf(BloomCategory.MAPLE)
        private val OBSERVATION_ORDER = compareByDescending<Triple<String, BloomCategory, BloomObservation>> {
            it.third.fullBloomOn != null
        }.thenBy {
            it.third.floweringOn ?: LocalDate.MAX
        }.thenBy {
            it.third.obsPlace
        }
    }
}
