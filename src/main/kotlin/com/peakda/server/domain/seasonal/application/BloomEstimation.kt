package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.seasonal.entity.Estimator
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 단일 추정기의 산출 결과이자 융합 전 중간 표현. `SeasonalBloomEstimate` 적재의 원천이 된다.
 *
 * [peakStartDate]·[peakEndDate] 가 모두 있으면 [peakDurationDays] 가 양 끝 포함 일수로 파생된다 (올해 만개 시기/만개 지속일 UI).
 */
data class BloomEstimation(
    val estimator: Estimator,
    val status: BloomStatus,
    val confidence: Double,
    val peakStartDate: LocalDate? = null,
    val peakEndDate: LocalDate? = null,
    val gddRatio: Double? = null,
    val evidence: String? = null,
) {
    val peakDurationDays: Int?
        get() = if (peakStartDate != null && peakEndDate != null) {
            (ChronoUnit.DAYS.between(peakStartDate, peakEndDate) + 1).toInt()
        } else {
            null
        }
}
