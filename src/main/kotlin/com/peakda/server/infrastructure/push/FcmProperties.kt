package com.peakda.server.infrastructure.push

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("app.push")
data class FcmProperties(
    val enabled: Boolean = false,
    val projectId: String? = null,
    val serviceAccountLocation: String? = null,
    val serviceAccountBase64: String? = null,
    val retry: Retry = Retry(),
) {

    /**
     * 일시 장애(INTERNAL·UNAVAILABLE) 응답에만 적용한다.
     * 발송은 알림 리스너와 배치 스레드에서 동기로 일어나므로, 한 묶음이 붙잡는 시간이
     * 길어지지 않도록 시도 횟수와 백오프를 짧게 잡는다.
     */
    data class Retry(
        val maxAttempts: Int = 2,
        val initialBackoff: Duration = Duration.ofSeconds(1),
        val multiplier: Double = 2.0,
    )
}
