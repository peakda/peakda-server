package com.peakda.server.infrastructure.external.common

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * provider 단위 단기 rate limit. data.go.kr 동시 호출 폭주를 막기 위한 token bucket 설정으로,
 * provider 마다 [permits] / [period] 비율로 토큰을 발급한다.
 */
@ConfigurationProperties(prefix = "external.rate-limit")
data class ExternalRateLimitProperties(
    val enabled: Boolean = false,
    val providers: Map<String, ProviderLimit> = emptyMap(),
) {
    fun limitFor(provider: String): ProviderLimit? = providers[provider]

    data class ProviderLimit(
        val permits: Int = 0,
        val period: Duration = Duration.ofSeconds(1),
        val timeout: Duration = Duration.ofMillis(500),
    )
}
