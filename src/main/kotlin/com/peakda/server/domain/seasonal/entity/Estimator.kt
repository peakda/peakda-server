package com.peakda.server.domain.seasonal.entity

/**
 * [BloomStatus] 를 산출한 추정기 종류. 융합 결과에서 어떤 신호가 채택됐는지 기록한다.
 */
enum class Estimator {
    GDD,
    FESTIVAL,
    CALENDAR,
    USER_RECORD,
}
