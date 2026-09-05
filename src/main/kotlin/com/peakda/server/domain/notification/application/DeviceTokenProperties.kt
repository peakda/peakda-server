package com.peakda.server.domain.notification.application

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "peakda.notification.device-token")
data class DeviceTokenProperties(
    /** 사용자당 보관할 최대 토큰 수. 초과분은 오래된 것부터 지운다. */
    val maxPerUser: Int = 10,

    /**
     * 마지막 등록·갱신 이후 이 일수가 지난 토큰을 정리한다.
     * FCM 이 미사용 토큰을 무효화하는 주기(270일)에 맞춘다.
     */
    val retentionDays: Long = 270,
)
