package com.peakda.server.infrastructure.external.common

import org.springframework.http.HttpRequest
import org.springframework.http.client.support.HttpRequestWrapper
import java.net.URI

class UriMutatingHttpRequest(
    request: HttpRequest,
    private val uri: URI,
) : HttpRequestWrapper(request) {
    override fun getURI(): URI = uri
}
