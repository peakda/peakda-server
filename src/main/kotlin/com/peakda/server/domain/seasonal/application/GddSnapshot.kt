package com.peakda.server.domain.seasonal.application

import java.time.LocalDate

data class GddSnapshot(
    val stationId: String,
    val accumulated: Double,
    /** 예보로 예측한 절정 시작 예상일. 이미 절정에 들어섰거나 예보 범위 밖이면 null. */
    val projectedPeakStartDate: LocalDate? = null,
    /** 예보로 예측한 절정 종료 예상일. */
    val projectedPeakEndDate: LocalDate? = null,
)
