package com.peakda.server.domain.seasonal.application

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 온디맨드 만개 캘린더 조회 설정.
 */
@ConfigurationProperties(prefix = "peakda.timing")
data class BloomCalendarProperties(
    /** 캘린더 시뮬레이션 기간(오늘부터 며칠). */
    val calendarHorizonDays: Long = 30,
)
