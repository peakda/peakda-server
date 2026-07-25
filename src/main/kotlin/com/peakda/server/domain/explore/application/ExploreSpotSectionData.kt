package com.peakda.server.domain.explore.application

import com.peakda.server.domain.seasonal.entity.SeasonalBloomEstimate
import org.springframework.data.domain.Page

/**
 * 스팟 섹션 한 건의 조회 결과. [page] 는 명소 단위로 페이징한 명소 id 이고,
 * [representativeByAttraction] 은 그 명소에서 대표로 채택한 카테고리 추정이다.
 */
internal data class ExploreSpotSectionData(
    val page: Page<Long>,
    val representativeByAttraction: Map<Long, SeasonalBloomEstimate>,
)
