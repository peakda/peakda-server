package com.peakda.server.domain.seasonal.application.estimator

import com.peakda.server.domain.seasonal.application.BloomEstimation
import com.peakda.server.domain.seasonal.application.BloomEstimationContext
import com.peakda.server.domain.seasonal.entity.Estimator

/**
 * 개화 상태 추정기. 각 신호(GDD·축제·달력·사용자기록)별 구현이 독립적으로 [estimate] 한다.
 *
 * null 반환은 "이 신호로는 판단 불가/비활성"을 의미하며 융합에서 제외된다.
 */
interface BloomEstimator {
    /** 이 추정기가 사용하는 신호 종류. 융합 결과의 `chosenEstimator` 기록에 쓰인다. */
    val estimator: Estimator

    fun estimate(context: BloomEstimationContext): BloomEstimation?
}
