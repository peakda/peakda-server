package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.seasonal.entity.SeasonalBloomEstimate

/**
 * 한 명소의 여러 카테고리 추정 중 대표 1건을 고르는 정렬 (앞이 대표).
 *
 * 이번 시즌이 남은 카테고리를 먼저 보고, 그 안에서 신뢰도를 1차 키로 쓴다.
 *
 * 신뢰도만으로 줄 세우면 관측이 붙은 벚꽃의 늦었다(0.9)가 달력뿐인 단풍의 절정(0.4)을 이겨,
 * 가을 내내 "늦었다 벚꽃"이 대표가 된다. 반대로 상태를 1차 키로 두면 신뢰도 0.3 짜리 PEAK 가
 * 0.9 짜리 PREPARING 을 이겨, 카테고리 하나만 절정으로 잡혀도 명소 전체가 만개로 보인다.
 * 그래서 끝났거나 아직 시즌이 아닌 상태만 뒤로 미루고, 나머지는 기존대로 신뢰도 순이다.
 *
 * 상세 배너·핀 프리뷰 뱃지·검색 뱃지·찜 카드가 같은 기준을 쓰도록 한 곳에 둔다.
 */
object BloomEstimateOrdering {

    val REPRESENTATIVE_FIRST: Comparator<SeasonalBloomEstimate> =
        compareBy<SeasonalBloomEstimate> { seasonRank(it.status) }
            .thenByDescending { it.confidence }
            .thenBy { statusRank(it.status) }

    /** 지금 찾아갈 만한 상태(이르다~절정)를 이미 지났거나 아직 시즌이 아닌 상태보다 앞세운다. */
    private fun seasonRank(status: BloomStatus): Int = when (status) {
        BloomStatus.PREPARING, BloomStatus.STARTED, BloomStatus.PEAK -> 0
        BloomStatus.ENDED, BloomStatus.BEFORE_SEASON -> 1
    }

    /** 신뢰도가 같을 때의 순서. 이미 진 꽃(ENDED)보다 아직 시즌이 아닌 꽃(BEFORE_SEASON)을 더 뒤에 둔다. */
    private fun statusRank(status: BloomStatus): Int = when (status) {
        BloomStatus.PEAK -> 0
        BloomStatus.STARTED -> 1
        BloomStatus.PREPARING -> 2
        BloomStatus.ENDED -> 3
        BloomStatus.BEFORE_SEASON -> 4
    }
}
