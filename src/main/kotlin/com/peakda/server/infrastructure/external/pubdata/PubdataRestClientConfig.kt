package com.peakda.server.infrastructure.external.pubdata

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
class PubdataRestClientConfig(
    private val pubdataProperties: PubdataProperties,
    private val dataGoKrProperties: DataGoKrProperties,
    private val restClientBuilder: RestClient.Builder,
    private val quotaService: QuotaService,
    private val rateLimiterRegistry: ProviderRateLimiterRegistry,
    private val meterRegistry: MeterRegistry,
) {
    @Bean
    @Qualifier("festivalRestClient")
    fun festivalRestClient(): RestClient {
        val service = "PublicCultureFestival"
        return ExternalRestClientFactory.create(
            builder = restClientBuilder,
            baseUrl = pubdataProperties.festival.baseUrl,
            properties = dataGoKrProperties,
            interceptors = listOf(
                RateLimitInterceptor(PROVIDER, rateLimiterRegistry, meterRegistry),
                QuotaGuardInterceptor(PROVIDER, service, quotaService, meterRegistry),
                ServiceKeyInterceptor(pubdataProperties.festival.serviceKey),
                PubdataJsonInterceptor(),
                ExternalApiLoggingInterceptor(PROVIDER, service),
            ),
        )
    }

    companion object {
        private const val PROVIDER = "PUBDATA"
    }
}
