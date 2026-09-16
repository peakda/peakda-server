package com.peakda.server.domain.seasonal.application.estimator

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 사용자 기록 신호(신호 D) 튜닝값. 운영 중 yml 로 조정한다.
 *
 * 명소형 융합의 [UserRecordBloomEstimator] 와 동네형 산출의
 * [com.peakda.server.domain.seasonal.application.LocalSpotBloomResolver] 가 [maxAgeDays] 를 공유한다.
 * 같은 "사용자 기록" 신호인데 스팟 유형에 따라 유효 기간이 달라지면 안 되기 때문이다.
 */
@ConfigurationProperties(prefix = "peakda.timing.user-record")
data class UserRecordEstimatorProperties(
    val enabled: Boolean = true,
    /** 최신(0일 경과) 기록의 신뢰도. */
    val baseConfidence: Double = 0.75,
    /** [maxAgeDays] 경과 시점의 신뢰도(하한). */
    val minConfidence: Double = 0.3,
    /** 관측일이 이보다 오래된 기록은 신호로 쓰지 않는다. 지난 계절의 절정 기록이 현재 상태로 남는 것을 막는다. */
    val maxAgeDays: Long = 14,
    /** 카테고리 매칭 대상으로 훑을 스팟당 최근 게시 기록 수. */
    val lookbackRecords: Int = 20,
)
