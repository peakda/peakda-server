package com.peakda.server.domain.seasonal.application

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 절정 구간 기준 상태 판정 윈도우. 운영 중 yml 로 조정한다.
 *
 * 두 값은 절정 시작일로부터 거슬러 올라간 일수이므로 [earlyWindowDays] 가 [startedWindowDays]
 * 보다 커야 개화전 → 이르다 → 시작 순서가 유지된다.
 */
@ConfigurationProperties(prefix = "peakda.timing.status-window")
data class BloomStatusWindowProperties(
    /** 절정 시작 며칠 전부터 시작(STARTED)으로 볼지. */
    val startedWindowDays: Long = 7,
    /** 절정 시작 며칠 전부터 이르다(PREPARING)로 볼지. 이보다 멀면 개화전(BEFORE_SEASON). */
    val earlyWindowDays: Long = 14,
)
