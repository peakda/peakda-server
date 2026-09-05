package com.peakda.server.domain.seasonal.application

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 상태 융합 정책 튜닝값. 운영 중 yml 로 조정한다.
 */
@ConfigurationProperties(prefix = "peakda.timing.fusion")
data class BloomFusionProperties(
    /** 동일 상태에 동의하는 추정기마다 더해지는 신뢰도 가산. */
    val agreementBonus: Double = 0.1,
    /** 가산 후 신뢰도 상한. */
    val agreementBonusCap: Double = 1.0,
    /** 상위 두 추정의 신뢰도 차가 이 값 미만이면 보수적 상태를 채택한다. */
    val tieBreakConfidenceMargin: Double = 0.1,
)
