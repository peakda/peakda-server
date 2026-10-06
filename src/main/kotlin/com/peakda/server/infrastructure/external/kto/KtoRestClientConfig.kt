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
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration

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

    /**
     * 관광공사 이미지 서버. API 가 아니라 serviceKey·쿼터 인터셉터를 붙이지 않고, 호출 간격만 따로 제한한다.
     * 상태 코드만 보므로 타임아웃을 짧게 둬서 서버 장애 때 잡이 오래 붙잡히지 않게 한다.
     */
    @Bean
    @Qualifier("ktoImageRestClient")
    fun ktoImageRestClient(): RestClient {
        return restClientBuilder.clone()
            .requestFactory(
                SimpleClientHttpRequestFactory().apply {
                    setConnectTimeout(IMAGE_CONNECT_TIMEOUT)
                    setReadTimeout(IMAGE_READ_TIMEOUT)
                },
            )
            .requestInterceptors { it.add(RateLimitInterceptor(IMAGE_PROVIDER, rateLimiterRegistry, meterRegistry)) }
            .build()
    }

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
        private const val IMAGE_PROVIDER = "KTO_IMAGE"
        private val IMAGE_CONNECT_TIMEOUT = Duration.ofSeconds(2)
        private val IMAGE_READ_TIMEOUT = Duration.ofSeconds(3)
    }
}
