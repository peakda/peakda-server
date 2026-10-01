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
            throw tooManyRequestsException(response.headers.getFirst("Retry-After"))
        }
        .body<String>()
        .orEmpty()

    errorDecoder.throwIfXmlError(rawBody)

    // 전국 표준데이터 API는 response wrapper 없이 header/body를 반환한다.
    val root = objectMapper.readTree(rawBody)
    val envelope = if (root.has("response")) {
        objectMapper.readValue<DataGoKrEnvelope<T>>(rawBody)
    } else {
        DataGoKrEnvelope(objectMapper.readValue<DataGoKrResponse<T>>(rawBody))
    }
    return errorDecoder.decode(envelope)
}

/**
 * HTTP 429 분류.
 *
 * - Retry-After 가 있으면 초 단위 속도 제한으로 보고 transient(UNAVAILABLE)로 재시도한다.
 *   형식을 읽지 못해도 헤더가 있다는 사실 자체가 "잠시 뒤 다시"라는 뜻이므로 같은 취급을 한다.
 * - Retry-After 가 없으면 data.go.kr 게이트웨이의 일일 호출 한도 초과로 본다. 같은 날 재시도해도
 *   계속 429 이고 남은 한도만 태우므로 QUOTA_EXCEEDED(permanent)로 즉시 전파한다.
 *   초 단위 폭주는 provider rate limiter(token bucket)가 먼저 막는다.
 */
@PublishedApi
internal fun tooManyRequestsException(retryAfterHeader: String?): ExternalApiException {
    val retryAfter = parseRetryAfter(retryAfterHeader)
    if (retryAfterHeader.isNullOrBlank()) {
        return ExternalApiException(
            ExternalApiErrorCode.EXTERNAL_API_QUOTA_EXCEEDED,
            "외부 API 호출 한도 초과 (HTTP 429)",
        )
    }
    return ExternalApiException(
        ExternalApiErrorCode.EXTERNAL_API_UNAVAILABLE,
        "외부 API rate limit (HTTP 429) Retry-After=$retryAfterHeader",
        retryAfter = retryAfter,
    )
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
