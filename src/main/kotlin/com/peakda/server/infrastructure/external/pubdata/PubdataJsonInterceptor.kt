package com.peakda.server.infrastructure.external.pubdata

import com.peakda.server.infrastructure.external.common.UriMutatingHttpRequest
import com.peakda.server.infrastructure.external.common.UriQueryAppender
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse

class PubdataJsonInterceptor : ClientHttpRequestInterceptor {
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        val uri = UriQueryAppender.appendEncoded(request.uri, TYPE_PARAM, TYPE_JSON)
        return execution.execute(UriMutatingHttpRequest(request, uri), body)
    }

    companion object {
        private const val TYPE_PARAM = "type"
        private const val TYPE_JSON = "json"
    }
}
