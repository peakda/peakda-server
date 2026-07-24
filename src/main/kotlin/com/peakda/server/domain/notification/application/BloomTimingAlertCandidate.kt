package com.peakda.server.domain.notification.application

import com.peakda.server.domain.seasonal.entity.BloomCategory
import java.time.LocalDate

/** 만개 임박 알림 생성과 푸시에 공통으로 사용하는 후보 데이터. */
data class BloomTimingAlertCandidate(
    val userId: Long,
    val spotId: Long,
    val spotName: String,
    val bloomCategory: BloomCategory,
    val peakStartDate: LocalDate,
    val daysUntilPeak: Long,
) {
    val peakYear: Int
        get() = peakStartDate.year
}
