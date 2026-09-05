package com.peakda.server.domain.explore.application

import com.peakda.server.domain.attraction.entity.Attraction
import com.peakda.server.domain.spot.entity.SpotFavorite

/**
 * 여러 스팟 섹션이 공유하는 배치 조회 결과. 섹션마다 명소·스팟·찜을 다시 조회하지 않도록
 * 명소 id 를 합쳐 한 번에 읽어 둔 것이다.
 */
internal data class ExploreSpotAssembly(
    val attractionsById: Map<Long, Attraction>,
    val spotIdByAttraction: Map<Long, Long>,
    val favoritesBySpot: Map<Long, SpotFavorite>,
)
