package com.peakda.server.domain.seasonal.application.estimator

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 사용자 기록 추정기(신호 D) 튜닝값. 운영 중 yml 로 조정한다.
 */
@ConfigurationProperties(prefix = "peakda.timing.user-record")
data class UserRecordEstimatorProperties(
    val enabled: Boolean = true,
    /** 최신(0일 경과) 기록의 신뢰도. */
    val baseConfidence: Double = 0.75,
    /** [maxAgeDays] 경과 시점의 신뢰도(하한). */
    val minConfidence: Double = 0.3,
    /** 이보다 오래된 기록은 신호로 쓰지 않는다. */
    val maxAgeDays: Long = 14,
    /** 카테고리 매칭 대상으로 훑을 스팟당 최근 게시 기록 수. */
    val lookbackRecords: Int = 20,
)
