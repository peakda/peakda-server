package com.peakda.server.infrastructure.external.datagokr

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.peakda.server.infrastructure.external.common.ExternalApiErrorCode
import com.peakda.server.infrastructure.external.common.ExternalApiException
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

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
            val retryAfterHeader = response.headers.getFirst("Retry-After")
            val retryAfter = parseRetryAfter(retryAfterHeader)
            val detail = if (retryAfterHeader != null) " Retry-After=$retryAfterHeader" else ""
            throw ExternalApiException(
                ExternalApiErrorCode.EXTERNAL_API_UNAVAILABLE,
                "외부 API rate limit (HTTP 429)$detail",
                retryAfter = retryAfter,
            )
        }
        .body<String>()
        .orEmpty()

    errorDecoder.throwIfXmlError(rawBody)

    val envelope = objectMapper.readValue<DataGoKrEnvelope<T>>(rawBody)
    return errorDecoder.decode(envelope)
}

@PublishedApi
internal fun parseRetryAfter(header: String?): Duration? {
    if (header.isNullOrBlank()) return null
    header.trim().toLongOrNull()?.let { seconds ->
        return if (seconds >= 0) Duration.ofSeconds(seconds) else null
    }
    return try {
        val target = ZonedDateTime.parse(header.trim(), DateTimeFormatter.RFC_1123_DATE_TIME)
        val delta = Duration.between(ZonedDateTime.now(ZoneOffset.UTC), target)
        if (delta.isNegative) Duration.ZERO else delta
    } catch (_: DateTimeParseException) {
        null
    }
}
