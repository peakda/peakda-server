package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.seasonal.entity.SeasonalBloomEstimate

/**
 * 한 명소의 여러 카테고리 추정 중 대표 1건을 고르는 정렬 (앞이 대표).
 *
 * 신뢰도가 1차 키다. 상태를 1차 키로 두면 신뢰도 0.3 짜리 PEAK 가 0.9 짜리 PREPARING 을 이겨,
 * 카테고리 하나만 절정으로 잡혀도 명소 전체가 만개로 보인다. 신뢰도가 같을 때만 사용자에게 더
 * 확정적인 상태를 앞세운다.
 *
 * 상세 배너·핀 프리뷰 뱃지·검색 뱃지·찜 카드가 같은 기준을 쓰도록 한 곳에 둔다.
 */
object BloomEstimateOrdering {

    val REPRESENTATIVE_FIRST: Comparator<SeasonalBloomEstimate> =
        compareByDescending<SeasonalBloomEstimate> { it.confidence }
            .thenBy { statusRank(it.status) }

    /** 지금 볼 만한 순서. 이미 진 꽃(ENDED)보다 아직 시즌이 아닌 꽃(BEFORE_SEASON)을 더 뒤에 둔다. */
    private fun statusRank(status: BloomStatus): Int = when (status) {
        BloomStatus.PEAK -> 0
        BloomStatus.STARTED -> 1
        BloomStatus.PREPARING -> 2
        BloomStatus.ENDED -> 3
        BloomStatus.BEFORE_SEASON -> 4
    }
}
