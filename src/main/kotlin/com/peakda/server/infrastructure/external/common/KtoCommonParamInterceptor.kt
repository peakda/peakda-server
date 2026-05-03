package com.peakda.server.infrastructure.external.common

import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse

class KtoCommonParamInterceptor(
    private val mobileApp: String,
) : ClientHttpRequestInterceptor {
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        val uri = request.uri
            .let { UriQueryAppender.appendEncoded(it, "MobileOS", "ETC") }
            .let { UriQueryAppender.appendEncoded(it, "MobileApp", mobileApp) }
            .let { UriQueryAppender.appendEncoded(it, "_type", "json") }

        return execution.execute(UriMutatingHttpRequest(request, uri), body)
    }
}
