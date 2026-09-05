package com.peakda.server.infrastructure.external.common

import com.peakda.server.infrastructure.external.datagokr.DataGoKrProperties
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient

object ExternalRestClientFactory {
    fun create(
        builder: RestClient.Builder,
        baseUrl: String,
        properties: DataGoKrProperties,
        interceptors: List<ClientHttpRequestInterceptor>,
    ): RestClient {
        return builder.clone()
            .baseUrl(baseUrl)
            .requestFactory(requestFactory(properties))
            .requestInterceptors { it.addAll(interceptors) }
            .build()
    }

    private fun requestFactory(properties: DataGoKrProperties): ClientHttpRequestFactory {
        return SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(properties.connectTimeout)
            setReadTimeout(properties.readTimeout)
        }
    }
}
