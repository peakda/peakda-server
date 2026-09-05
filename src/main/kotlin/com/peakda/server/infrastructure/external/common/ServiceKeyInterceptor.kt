package com.peakda.server.infrastructure.external.common

import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse

class ServiceKeyInterceptor(
    private val serviceKey: String,
) : ClientHttpRequestInterceptor {
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        val uri = UriQueryAppender.appendRaw(request.uri, SERVICE_KEY_PARAM, serviceKey)
        return execution.execute(UriMutatingHttpRequest(request, uri), body)
    }

    companion object {
        private const val SERVICE_KEY_PARAM = "serviceKey"
    }
}
