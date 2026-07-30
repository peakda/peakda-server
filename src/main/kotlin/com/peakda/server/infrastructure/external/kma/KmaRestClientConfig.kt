package com.peakda.server.infrastructure.external.kma

import com.peakda.server.infrastructure.external.datagokr.DataGoKrProperties
import com.peakda.server.infrastructure.external.common.ExternalApiLoggingInterceptor
import com.peakda.server.infrastructure.external.common.ExternalRestClientFactory
import com.peakda.server.infrastructure.external.common.ProviderRateLimiterRegistry
import com.peakda.server.infrastructure.external.common.QuotaGuardInterceptor
import com.peakda.server.infrastructure.external.common.QuotaService
import com.peakda.server.infrastructure.external.common.RateLimitInterceptor
import com.peakda.server.infrastructure.external.common.ServiceKeyInterceptor
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class KmaRestClientConfig(
    private val kmaProperties: KmaProperties,
    private val dataGoKrProperties: DataGoKrProperties,
    private val restClientBuilder: RestClient.Builder,
    private val quotaService: QuotaService,
    private val rateLimiterRegistry: ProviderRateLimiterRegistry,
    private val meterRegistry: MeterRegistry,
) {
    @Bean
    @Qualifier("vilageFcstRestClient")
    fun vilageFcstRestClient(): RestClient = kmaRestClient(kmaProperties.baseUrl.vilageFcst, "VilageFcstInfoService")

    @Bean
    @Qualifier("midFcstRestClient")
    fun midFcstRestClient(): RestClient = kmaRestClient(kmaProperties.baseUrl.midFcst, "MidFcstInfoService")

    @Bean
    @Qualifier("asosDalyRestClient")
    fun asosDalyRestClient(): RestClient = kmaRestClient(kmaProperties.baseUrl.asosDaly, "AsosDalyInfoService")

    private fun kmaRestClient(baseUrl: String, service: String): RestClient {
        return ExternalRestClientFactory.create(
            builder = restClientBuilder,
            baseUrl = baseUrl,
            properties = dataGoKrProperties,
            interceptors = listOf(
                RateLimitInterceptor(PROVIDER, rateLimiterRegistry, meterRegistry),
                QuotaGuardInterceptor(PROVIDER, service, quotaService, meterRegistry),
                ServiceKeyInterceptor(kmaProperties.serviceKey),
                KmaJsonInterceptor(),
                ExternalApiLoggingInterceptor(PROVIDER, service),
            ),
        )
    }

    companion object {
        private const val PROVIDER = "KMA"
    }
}
