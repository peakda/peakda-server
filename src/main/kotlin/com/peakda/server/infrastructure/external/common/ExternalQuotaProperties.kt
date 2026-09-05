package com.peakda.server.infrastructure.external.common

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * provider/service 단위 일일 호출 한도. data.go.kr 은 service-key 당 service 별로 quota 를 부여하므로
 * `quota:{provider}:{service}:{yyyy-MM-dd}` 키로 INCR 카운팅한다. 한도 도달 시 외부 호출을 막아
 * 잡이 quota 를 모두 소진하기 전에 stop 한다.
 */
@ConfigurationProperties(prefix = "external.quota")
data class ExternalQuotaProperties(
    val enabled: Boolean = false,
    val services: Map<String, ServiceQuota> = emptyMap(),
) {
    /**
     * [key] 는 application.yml 에서 `KTO.KorService2`, `KMA.VilageFcstInfoService_2.0` 처럼
     * provider 와 service 이름을 dot 으로 묶어 lookup 한다.
     */
    fun limitFor(provider: String, service: String): Long? {
        return services["$provider.$service"]?.dailyLimit
    }

    data class ServiceQuota(
        val dailyLimit: Long = 0,
    )
}
