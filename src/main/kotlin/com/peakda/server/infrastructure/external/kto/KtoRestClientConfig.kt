package com.peakda.server.infrastructure.external.kto

import com.peakda.server.infrastructure.external.datagokr.DataGoKrProperties
import com.peakda.server.infrastructure.external.common.ExternalApiLoggingInterceptor
import com.peakda.server.infrastructure.external.common.ExternalRestClientFactory
import com.peakda.server.infrastructure.external.common.JsonOnlyInterceptor
import com.peakda.server.infrastructure.external.common.KtoCommonParamInterceptor
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
class KtoRestClientConfig(
    private val ktoProperties: KtoProperties,
    private val dataGoKrProperties: DataGoKrProperties,
    private val restClientBuilder: RestClient.Builder,
    private val quotaService: QuotaService,
    private val rateLimiterRegistry: ProviderRateLimiterRegistry,
    private val meterRegistry: MeterRegistry,
) {
    @Bean
    @Qualifier("korServiceRestClient")
    fun korServiceRestClient(): RestClient = ktoRestClient(ktoProperties.baseUrl.korService, "KorService2")

    @Bean
    @Qualifier("tatsCnctrRestClient")
    fun tatsCnctrRestClient(): RestClient = ktoRestClient(ktoProperties.baseUrl.tatsCnctr, "TatsCnctrRateService")

    @Bean
    @Qualifier("dataLabRestClient")
    fun dataLabRestClient(): RestClient = ktoRestClient(ktoProperties.baseUrl.dataLab, "DataLabService")

    @Bean
    @Qualifier("photoGalleryRestClient")
    fun photoGalleryRestClient(): RestClient = ktoRestClient(ktoProperties.baseUrl.photo, "PhotoGalleryService1")

    @Bean
    @Qualifier("durunubiRestClient")
    fun durunubiRestClient(): RestClient = ktoRestClient(ktoProperties.baseUrl.durunubi, "Durunubi")

    private fun ktoRestClient(baseUrl: String, service: String): RestClient {
        return ExternalRestClientFactory.create(
            builder = restClientBuilder,
            baseUrl = baseUrl,
            properties = dataGoKrProperties,
            interceptors = listOf(
                RateLimitInterceptor(PROVIDER, rateLimiterRegistry, meterRegistry),
                QuotaGuardInterceptor(PROVIDER, service, quotaService, meterRegistry),
                ServiceKeyInterceptor(ktoProperties.serviceKey),
                KtoCommonParamInterceptor(dataGoKrProperties.mobileApp),
                JsonOnlyInterceptor(),
                ExternalApiLoggingInterceptor(PROVIDER, service),
            ),
        )
    }

    companion object {
        private const val PROVIDER = "KTO"
    }
}
