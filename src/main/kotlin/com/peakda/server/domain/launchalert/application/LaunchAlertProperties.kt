package com.peakda.server.domain.launchalert.application

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * @property consentVersion 랜딩 페이지에 보이는 개인정보 수집·이용 안내 문구의 버전. 문구를 바꾸면 함께 올린다.
 * @property rateLimit 같은 IP 의 신청 횟수 제한. 비로그인 공개 API 라 자동 대량 등록을 막는다.
 */
@ConfigurationProperties(prefix = "app.launch-alert")
data class LaunchAlertProperties(
    val consentVersion: String = "2026-10-21",
    val rateLimit: RateLimit = RateLimit(),
) {
    data class RateLimit(
        val maxPerWindow: Long = 10,
        val window: Duration = Duration.ofHours(1),
    )
}
