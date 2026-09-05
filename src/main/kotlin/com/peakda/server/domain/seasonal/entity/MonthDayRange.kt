package com.peakda.server.domain.seasonal.entity

import java.time.MonthDay

/**
 * 연도와 무관한 월·일 구간 (카테고리별 평년 절정 기간 등에 사용).
 *
 * 동백처럼 연말을 넘어가는 구간(예: 12/1 ~ 3/15)도 표현할 수 있도록 [contains] 가 wrap-around 를 처리한다.
 */
data class MonthDayRange(
    val from: MonthDay,
    val to: MonthDay,
) {
    /**
     * [monthDay] 가 from~to 구간에 포함되는지. from 이 to 보다 늦으면 연말을 넘는 구간으로 해석한다.
     */
    fun contains(monthDay: MonthDay): Boolean =
        if (from <= to) {
            monthDay >= from && monthDay <= to
        } else {
            monthDay >= from || monthDay <= to
        }
}
