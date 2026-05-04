package com.peakda.server.infrastructure.external.kma

import com.peakda.server.infrastructure.external.common.DataGoKrProperties
import com.peakda.server.infrastructure.external.common.ExternalApiLoggingInterceptor
import com.peakda.server.infrastructure.external.common.ExternalRestClientFactory
import com.peakda.server.infrastructure.external.common.JsonOnlyInterceptor
import com.peakda.server.infrastructure.external.common.ServiceKeyInterceptor
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class KmaRestClientConfig(
    private val kmaProperties: KmaProperties,
    private val dataGoKrProperties: DataGoKrProperties,
    private val restClientBuilder: RestClient.Builder,
) {
    @Bean
    @Qualifier("vilageFcstRestClient")
    fun vilageFcstRestClient(): RestClient = kmaRestClient(kmaProperties.baseUrl.vilageFcst, "VilageFcstInfoService")

    @Bean
    @Qualifier("midFcstRestClient")
    fun midFcstRestClient(): RestClient = kmaRestClient(kmaProperties.baseUrl.midFcst, "MidFcstInfoService")

    private fun kmaRestClient(baseUrl: String, service: String): RestClient {
        return ExternalRestClientFactory.create(
            builder = restClientBuilder,
            baseUrl = baseUrl,
            properties = dataGoKrProperties,
            interceptors = listOf(
                ServiceKeyInterceptor(kmaProperties.serviceKey),
                JsonOnlyInterceptor(),
                ExternalApiLoggingInterceptor("KMA", service),
            ),
        )
    }
}
