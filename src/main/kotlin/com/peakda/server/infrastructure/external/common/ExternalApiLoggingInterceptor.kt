package com.peakda.server.infrastructure.external.common

import org.slf4j.LoggerFactory
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import kotlin.system.measureTimeMillis

class ExternalApiLoggingInterceptor(
    private val provider: String,
    private val service: String,
) : ClientHttpRequestInterceptor {
    private val log = LoggerFactory.getLogger(this::class.java)

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        lateinit var response: ClientHttpResponse
        val elapsedMs = measureTimeMillis {
            response = execution.execute(request, body)
        }

        log.info(
            "[external] provider={} service={} op={} status={} ms={} uri={}",
            provider,
            service,
            operationName(request),
            response.statusCode.value(),
            elapsedMs,
            UriQueryAppender.maskServiceKey(request.uri),
        )
        return response
    }

    private fun operationName(request: HttpRequest): String {
        return request.uri.path.substringAfterLast('/').ifBlank { "unknown" }
    }
}
