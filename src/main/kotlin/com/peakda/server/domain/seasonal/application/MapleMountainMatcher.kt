package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.attraction.entity.Attraction
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.infrastructure.external.kma.flower.MapleObservationMountainCatalog
import org.springframework.stereotype.Component

/**
 * 명소가 기상청 유명산 단풍 관측 산에 속하는지 판정한다.
 *
 * 단풍 관측은 산 정상부터의 진행을 보므로 관측소 권역이 아니라 산에만 연결한다.
 * - 명소 제목에 산 이름이 들어 있고, 명소의 법정동 시도가 산이 걸친 시도 안에 있어야 한다 (동명이산 배제).
 * - 제목에 단풍이 아닌 다른 꽃 키워드가 있으면(예: `속리산연꽃단지`) 그 꽃의 명소로 보고 연결하지 않는다.
 * - 제목에 산별 제외어가 있으면(예: 지리산의 `사량도`) 같은 시도 안의 동명이산으로 보고 연결하지 않는다.
 */
@Component
class MapleMountainMatcher(
    catalog: MapleObservationMountainCatalog,
) {
    private val mountains: List<Mountain> = catalog.mountains.map { mountain ->
        Mountain(
            name = mountain.name,
            normalizedName = FestivalPlaceTokenizer.normalize(mountain.name),
            areaCodes = mountain.areaCodes,
            excludeKeywords = mountain.excludeKeywords.map(FestivalPlaceTokenizer::normalize),
        )
    }

    /** 명소가 속한 관측 산 이름. 해당 없으면 null. */
    fun mountainOf(attraction: Attraction): String? {
        val areaCode = attraction.legalDongAreaCode ?: return null
        val title = FestivalPlaceTokenizer.normalize(attraction.title)
        if (OTHER_FLOWER_HINTS.any { title.contains(it) }) return null
        return mountains.firstOrNull { mountain ->
            areaCode in mountain.areaCodes &&
                title.contains(mountain.normalizedName) &&
                mountain.excludeKeywords.none { title.contains(it) }
        }?.name
    }

    private data class Mountain(
        val name: String,
        val normalizedName: String,
        val areaCodes: Set<String>,
        val excludeKeywords: List<String>,
    )

    companion object {
        private val OTHER_FLOWER_HINTS: List<String> = BloomCategory.entries
            .filter { it != BloomCategory.MAPLE }
            .flatMap { it.keywordHints }
            .map(FestivalPlaceTokenizer::normalize)
            .distinct()
    }
}
