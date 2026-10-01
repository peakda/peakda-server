package com.peakda.server.domain.congestion.application

import java.time.LocalDate

/** 하루 단위 관광지 집중률 예측. [rate] 는 관광지 자체 기준의 0~100 상대 지수다(방문객 수 아님). */
data class DailyCongestion(
    val date: LocalDate,
    val rate: Double,
)
