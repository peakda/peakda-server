package com.peakda.server.domain.seasonal.application.estimator

import com.peakda.server.domain.seasonal.application.BloomEstimation
import com.peakda.server.domain.seasonal.application.BloomEstimationContext
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.seasonal.entity.Estimator
import org.springframework.stereotype.Component
import java.util.Locale

/**
 * 신호 A — GDD(생장도일) 누적기온 추정기.
 *
 * 단풍은 기온 하강에 반응해 GDD 메커니즘과 반대이므로 임계치를 두지 않고 신호에서 제외한다.
 * 예측 절정일이 없으면 null 로 두어 융합 단계가 달력·축제 구간을 승계하도록 한다.
 */
@Component
class GddBloomEstimator(
    private val properties: GddEstimatorProperties,
) : BloomEstimator {

    override val estimator = Estimator.GDD

    override fun estimate(context: BloomEstimationContext): BloomEstimation? {
        if (!properties.enabled) return null
        if (!context.category.temperatureDriven) return null
        val threshold = properties.thresholds[context.category.name] ?: return null
        val snapshot = context.gdd ?: return null

        val status = when {
            snapshot.accumulated >= threshold.end -> BloomStatus.ENDED
            snapshot.accumulated >= threshold.peak -> BloomStatus.PEAK
            snapshot.accumulated >= threshold.start -> BloomStatus.STARTED
            else -> BloomStatus.PREPARING
        }
        val gddRatio = threshold.peak.takeIf { it > 0.0 }?.let { snapshot.accumulated / it }
        val roundedAccumulated = String.format(Locale.ROOT, "%.1f", snapshot.accumulated)

        return BloomEstimation(
            estimator = Estimator.GDD,
            status = status,
            confidence = if (status == BloomStatus.ENDED) {
                properties.endedConfidence
            } else {
                properties.baseConfidence
            },
            peakStartDate = snapshot.projectedPeakStartDate,
            peakEndDate = snapshot.projectedPeakEndDate,
            gddRatio = gddRatio,
            evidence = "gdd:station=${snapshot.stationId},acc=$roundedAccumulated,tbase=${threshold.tBase}",
        )
    }
}
