package com.peakda.server.infrastructure.external.datagokr

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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
        .body<String>()
        .orEmpty()

    errorDecoder.throwIfXmlError(rawBody)

    val envelope = objectMapper.readValue<DataGoKrEnvelope<T>>(rawBody)
    return errorDecoder.decode(envelope)
}
