package com.peakda.server.domain.launchalert.application

/** 출시 알림 신청의 IP 별 횟수 제한. */
interface LaunchAlertRateLimiter {
    fun tryAcquire(clientIp: String): Boolean
}
