package com.peakda.server.domain.festival.application

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * 원천(공공데이터) 축제 날짜 문자열을 [LocalDate]로 정규화한다.
 *
 * `2026-05-01`·`20260501`처럼 구분자가 섞여 들어오므로 숫자만 남겨 8자리일 때만 해석한다.
 * 그 밖의 값은 null이다.
 * 파싱은 동기화 경계에서 한 번만 하고 조회·판정은 정규화 컬럼(`festivals.starts_on`·`ends_on`)을 쓴다.
 */
object FestivalDates {

    fun parse(value: String?): LocalDate? {
        if (value.isNullOrBlank()) return null
        val digits = value.filter { it.isDigit() }
        if (digits.length != DIGITS) return null
        return try {
            LocalDate.parse(digits, DateTimeFormatter.BASIC_ISO_DATE)
        } catch (e: DateTimeParseException) {
            null
        }
    }

    private const val DIGITS = 8
}
