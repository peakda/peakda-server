package com.peakda.server.domain.notification.application

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "peakda.notification.timing-alert")
data class BloomTimingAlertProperties(
    /** 만개 시작 며칠 전까지를 "임박"으로 보고 알림할지. (오늘 기준 1..leadDays 일 이내) */
    val leadDays: Long = 7,

    /** 만개 임박 알림 후보 페이지 크기. */
    val pageSize: Int = 500,
)
