package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.seasonal.application.estimator.BloomEstimator
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.seasonal.entity.Estimator
import org.springframework.stereotype.Service
import kotlin.math.abs

/**
 * 여러 추정기의 산출을 하나의 [BloomEstimation] 으로 융합한다.
 *
 * 1. 각 추정기를 호출해 비-null 결과만 모은다 (모두 비활성이면 null).
 * 2. 신뢰도 내림차순, 동률이면 카테고리별 추정기 우선순위(온도의존종 FESTIVAL>GDD>CALENDAR, 그 외 FESTIVAL>CALENDAR)로 정렬한다.
 * 3. 상위 두 결과의 상태가 다르고 신뢰도 차가 작으면 보수적 상태(STARTED>PEAK>PREPARING>ENDED)를 채택한다.
 * 4. 채택 상태에 동의하는 추정기 수만큼 신뢰도를 가산한다(상한 적용).
 */
@Service
class BloomStatusFusionService(
    private val estimators: List<BloomEstimator>,
    private val properties: BloomFusionProperties,
) {
    fun fuse(context: BloomEstimationContext): BloomEstimation? {
        val results = estimators.mapNotNull { it.estimate(context) }
        if (results.isEmpty()) return null

        val ranked = results.sortedWith(
            compareByDescending<BloomEstimation> { it.confidence }
                .thenBy { estimatorPriority(it.estimator, context.category.temperatureDriven) },
        )
        val top = ranked.first()
        val runnerUp = ranked.getOrNull(1)

        val base = if (
            runnerUp != null &&
            runnerUp.status != top.status &&
            abs(top.confidence - runnerUp.confidence) < properties.tieBreakConfidenceMargin &&
            conservativeRank(runnerUp.status) < conservativeRank(top.status)
        ) {
            runnerUp
        } else {
            top
        }

        val agreeing = results.count { it.status == base.status }
        val boosted = base.confidence + (agreeing - 1) * properties.agreementBonus
        val confidence = boosted.coerceAtMost(properties.agreementBonusCap).coerceAtMost(1.0)

        return base.copy(confidence = confidence)
    }

    /** 동률 신뢰도일 때 신호 신뢰도 우선순위 (작을수록 우선). */
    private fun estimatorPriority(estimator: Estimator, temperatureDriven: Boolean): Int = when (estimator) {
        Estimator.FESTIVAL -> 0
        Estimator.GDD -> if (temperatureDriven) 1 else 9
        Estimator.CALENDAR -> 2
        Estimator.USER_RECORD -> 3
    }

    /** 보수성 순위 (작을수록 보수적). 같은 신뢰도면 사용자에게 덜 단정적인 상태를 택한다. */
    private fun conservativeRank(status: BloomStatus): Int = when (status) {
        BloomStatus.STARTED -> 0
        BloomStatus.PEAK -> 1
        BloomStatus.PREPARING -> 2
        BloomStatus.ENDED -> 3
    }
}
