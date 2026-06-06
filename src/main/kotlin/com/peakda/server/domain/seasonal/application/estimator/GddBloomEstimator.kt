package com.peakda.server.domain.seasonal.application.estimator

import com.peakda.server.domain.seasonal.application.BloomEstimation
import com.peakda.server.domain.seasonal.application.BloomEstimationContext
import com.peakda.server.domain.seasonal.entity.Estimator
import org.springframework.stereotype.Component

/**
 * 신호 A — GDD(생장도일) 누적기온 추정기. **MVP 스캐폴딩**.
 *
 * 현재 날씨 테이블은 예보(미래)만 보관해 과거 실측 일평균기온이 없어 누적 GDD 를 계산할 수 없다.
 * KMA ASOS 과거관측 일자료 수집 파이프라인이 추가되면 활성화한다. 그 전까지는 항상 null 을 반환해 융합에서 제외된다.
 */
@Component
class GddBloomEstimator(
    private val properties: GddEstimatorProperties,
) : BloomEstimator {

    override val estimator = Estimator.GDD

    override fun estimate(context: BloomEstimationContext): BloomEstimation? {
        if (!properties.enabled) return null
        // TODO(ASOS): 1/1~baseDate 실측 일평균기온 누적 → properties.thresholds[category.name] 로 status·gddRatio 산출
        return null
    }
}
