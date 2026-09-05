package com.peakda.server.infrastructure.external.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpRequest
import org.springframework.http.HttpStatus
import org.springframework.mock.http.client.MockClientHttpRequest
import org.springframework.mock.http.client.MockClientHttpResponse
import java.net.URI

class ExternalApiInterceptorTest {
    @Test
    fun `serviceKey is appended as raw query string without double encoding`() {
        val serviceKey = "abc%2Bdef%3D%3D"
        val interceptor = ServiceKeyInterceptor(serviceKey)
        var captured: URI? = null

        interceptor.intercept(request("https://apis.data.go.kr/test?numOfRows=10"), ByteArray(0)) { req, _ ->
            captured = req.uri
            MockClientHttpResponse(ByteArray(0), HttpStatus.OK)
        }

        assertThat(captured.toString()).isEqualTo(
            "https://apis.data.go.kr/test?numOfRows=10&serviceKey=abc%2Bdef%3D%3D"
        )
    }

    @Test
    fun `serviceKey interceptor does not append duplicate key`() {
        val interceptor = ServiceKeyInterceptor("new-key")
        var captured: URI? = null

        interceptor.intercept(request("https://apis.data.go.kr/test?serviceKey=old-key"), ByteArray(0)) { req, _ ->
            captured = req.uri
            MockClientHttpResponse(ByteArray(0), HttpStatus.OK)
        }

        assertThat(captured.toString()).isEqualTo("https://apis.data.go.kr/test?serviceKey=old-key")
    }

    @Test
    fun `KTO common params are appended once`() {
        val interceptor = KtoCommonParamInterceptor("peakda")
        var captured: URI? = null

        interceptor.intercept(request("https://apis.data.go.kr/test?MobileOS=IOS"), ByteArray(0)) { req, _ ->
            captured = req.uri
            MockClientHttpResponse(ByteArray(0), HttpStatus.OK)
        }

        assertThat(captured.toString()).isEqualTo(
            "https://apis.data.go.kr/test?MobileOS=IOS&MobileApp=peakda"
        )
    }

    @Test
    fun `JSON only param is appended`() {
        val interceptor = JsonOnlyInterceptor()
        var captured: URI? = null

        interceptor.intercept(request("https://apis.data.go.kr/test"), ByteArray(0)) { req, _ ->
            captured = req.uri
            MockClientHttpResponse(ByteArray(0), HttpStatus.OK)
        }

        assertThat(captured.toString()).isEqualTo("https://apis.data.go.kr/test?_type=json")
    }

    private fun request(uri: String): HttpRequest {
        return MockClientHttpRequest(HttpMethod.GET, URI.create(uri))
    }
}
