package com.peakda.server.domain.seasonal.application.estimator

import com.peakda.server.domain.seasonal.application.BloomEstimation
import com.peakda.server.domain.seasonal.application.BloomEstimationContext
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.seasonal.entity.Estimator
import org.springframework.stereotype.Component

/**
 * 신호 E — 기상청 군락단지 개화 관측. 같은 관측지점 권역의 직접 관측을 최우선 신호로 쓴다.
 */
@Component
class ObservationBloomEstimator(
    private val properties: ObservationEstimatorProperties,
) : BloomEstimator {

    override val estimator = Estimator.OBSERVATION

    override fun estimate(context: BloomEstimationContext): BloomEstimation? {
        if (!properties.enabled) return null
        val snapshot = context.observation ?: return null
        val flowering = snapshot.floweringOn ?: return null

        val fullBloom = snapshot.fullBloomOn
        val peakEnd = fullBloom?.plusDays(properties.peakDurationDays)
        val status = when {
            peakEnd != null && context.baseDate.isAfter(peakEnd) -> BloomStatus.ENDED
            fullBloom != null && !context.baseDate.isBefore(fullBloom) -> BloomStatus.PEAK
            !context.baseDate.isBefore(flowering) -> BloomStatus.STARTED
            else -> BloomStatus.PREPARING
        }
        return BloomEstimation(
            estimator = Estimator.OBSERVATION,
            status = status,
            confidence = properties.baseConfidence,
            peakStartDate = fullBloom,
            peakEndDate = peakEnd,
            evidence = "observation:${snapshot.obsPlace},flowering=$flowering,fullBloom=$fullBloom",
        )
    }
}
