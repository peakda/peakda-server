package com.peakda.server.infrastructure.external.kto.image

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.web.client.ExpectedCount.never
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.anything
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withException
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.web.client.RestClient
import java.io.IOException

class KtoImageProbeClientTest {
    private val builder = RestClient.builder()
    private val server = MockRestServiceServer.bindTo(builder).build()
    private val client = KtoImageProbeClient(builder.build())

    @Test
    fun `첫 1바이트만 GET 으로 요청하고 2xx 면 이미지가 있다고 본다`() {
        server.expect(requestTo(URL))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header(HttpHeaders.RANGE, "bytes=0-0"))
            .andRespond(withStatus(HttpStatus.PARTIAL_CONTENT))

        assertThat(client.probe(URL)).isEqualTo(KtoImageProbeResult.AVAILABLE)
        server.verify()
    }

    @Test
    fun `404 와 410 은 파일이 없다고 본다`() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.NOT_FOUND))
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.GONE))

        assertThat(client.probe(URL)).isEqualTo(KtoImageProbeResult.MISSING)
        assertThat(client.probe(URL)).isEqualTo(KtoImageProbeResult.MISSING)
        server.verify()
    }

    @Test
    fun `5xx 와 연결 오류는 판단하지 않는다`() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE))
        server.expect(requestTo(URL)).andRespond(withException(IOException("timeout")))

        assertThat(client.probe(URL)).isEqualTo(KtoImageProbeResult.UNKNOWN)
        assertThat(client.probe(URL)).isEqualTo(KtoImageProbeResult.UNKNOWN)
        server.verify()
    }

    @Test
    fun `관광공사 이미지 서버가 아닌 주소는 요청하지 않는다`() {
        server.expect(never(), anything())

        assertThat(client.probe("http://169.254.169.254/latest/meta-data")).isEqualTo(KtoImageProbeResult.UNSUPPORTED)
        assertThat(client.probe("file:///etc/passwd")).isEqualTo(KtoImageProbeResult.UNSUPPORTED)
        assertThat(client.probe("not a url")).isEqualTo(KtoImageProbeResult.UNSUPPORTED)
        server.verify()
    }

    companion object {
        private const val URL = "http://tong.visitkorea.or.kr/cms/resource/30/2614830_image3_1.bmp"
    }
}
