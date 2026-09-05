package com.peakda.server.infrastructure.external.common

import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse

class JsonOnlyInterceptor : ClientHttpRequestInterceptor {
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        val uri = UriQueryAppender.appendEncoded(request.uri, "_type", "json")
        return execution.execute(UriMutatingHttpRequest(request, uri), body)
    }
}
