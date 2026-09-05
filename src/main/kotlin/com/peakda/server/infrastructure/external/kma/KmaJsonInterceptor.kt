package com.peakda.server.infrastructure.external.kma

import com.peakda.server.infrastructure.external.common.UriMutatingHttpRequest
import com.peakda.server.infrastructure.external.common.UriQueryAppender
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse

class KmaJsonInterceptor : ClientHttpRequestInterceptor {
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        val uri = UriQueryAppender.appendEncoded(request.uri, DATA_TYPE_PARAM, DATA_TYPE_JSON)
        return execution.execute(UriMutatingHttpRequest(request, uri), body)
    }

    companion object {
        private const val DATA_TYPE_PARAM = "dataType"
        private const val DATA_TYPE_JSON = "JSON"
    }
}
