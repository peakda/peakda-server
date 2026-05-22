package com.peakda.server.infrastructure.external.datagokr

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.peakda.server.infrastructure.external.common.ExternalApiErrorCode
import com.peakda.server.infrastructure.external.common.ExternalApiException
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

inline fun <reified T : Any> RestClient.getDataGoKrBody(
    objectMapper: ObjectMapper,
    errorDecoder: DataGoKrErrorDecoder,
    path: String,
    queryParams: Map<String, Any?> = emptyMap(),
): DataGoKrBody<T> {
    val rawBody = get()
        .uri { builder ->
            builder.path(path)
            queryParams.forEach { (name, value) ->
                when (value) {
                    null -> Unit
                    is Iterable<*> -> value.filterNotNull().forEach { builder.queryParam(name, it) }
                    else -> builder.queryParam(name, value)
                }
            }
            builder.build()
        }
        .retrieve()
        .onStatus({ it.value() == 429 }) { _, response ->
            val retryAfter = response.headers.getFirst("Retry-After")
            val detail = if (retryAfter != null) " Retry-After=$retryAfter" else ""
            throw ExternalApiException(
                ExternalApiErrorCode.EXTERNAL_API_UNAVAILABLE,
                "외부 API rate limit (HTTP 429)$detail",
            )
        }
        .body<String>()
        .orEmpty()

    errorDecoder.throwIfXmlError(rawBody)

    val envelope = objectMapper.readValue<DataGoKrEnvelope<T>>(rawBody)
    return errorDecoder.decode(envelope)
}
