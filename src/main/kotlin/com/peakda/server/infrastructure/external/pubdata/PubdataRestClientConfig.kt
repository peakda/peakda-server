package com.peakda.server.infrastructure.external.pubdata

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
class PubdataRestClientConfig(
    private val pubdataProperties: PubdataProperties,
    private val dataGoKrProperties: DataGoKrProperties,
    private val restClientBuilder: RestClient.Builder,
) {
    @Bean
    @Qualifier("festivalRestClient")
    fun festivalRestClient(): RestClient {
        return ExternalRestClientFactory.create(
            builder = restClientBuilder,
            baseUrl = pubdataProperties.festival.baseUrl,
            properties = dataGoKrProperties,
            interceptors = listOf(
                ServiceKeyInterceptor(pubdataProperties.festival.serviceKey),
                JsonOnlyInterceptor(),
                ExternalApiLoggingInterceptor("PUBDATA", "PublicCultureFestival"),
            ),
        )
    }
}
