package com.peakda.server.common.logging

import jakarta.servlet.DispatcherType
import jakarta.servlet.FilterChain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.util.WebUtils

class RequestIdFilterTest {

    private val filter = RequestIdFilter()

    @Test
    fun `Caddy 가 넘긴 ID 를 MDC 와 응답 헤더에 그대로 싣는다`() {
        val request = MockHttpServletRequest().apply {
            addHeader(RequestIdFilter.HEADER, "0b6f1c2e-4d5a-4f6b-9c7d-8e9f0a1b2c3d")
        }
        val response = MockHttpServletResponse()

        val seen = runCapturingMdc(request, response)

        assertThat(seen).isEqualTo("0b6f1c2e-4d5a-4f6b-9c7d-8e9f0a1b2c3d")
        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo(seen)
    }

    @Test
    fun `헤더가 없으면 새 ID 를 만든다`() {
        val response = MockHttpServletResponse()

        val seen = runCapturingMdc(MockHttpServletRequest(), response)

        assertThat(seen).isNotBlank()
        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo(seen)
    }

    @Test
    fun `로그에 그대로 들어가면 안 되는 헤더 값은 버리고 새로 만든다`() {
        val request = MockHttpServletRequest().apply {
            addHeader(RequestIdFilter.HEADER, "abc\n{\"level\":\"ERROR\"}")
        }

        val seen = runCapturingMdc(request, MockHttpServletResponse())

        assertThat(seen).doesNotContain("\n").doesNotContain("ERROR")
    }

    @Test
    fun `요청이 끝나면 MDC 에서 지운다`() {
        runCapturingMdc(MockHttpServletRequest(), MockHttpServletResponse())

        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull()
    }

    @Test
    fun `오류 디스패치에서도 처음 요청과 같은 ID 를 쓴다`() {
        val request = MockHttpServletRequest()
        val first = runCapturingMdc(request, MockHttpServletResponse())

        request.dispatcherType = DispatcherType.ERROR
        request.setAttribute(WebUtils.ERROR_REQUEST_URI_ATTRIBUTE, "/api/spots")
        val onError = runCapturingMdc(request, MockHttpServletResponse())

        assertThat(onError).isEqualTo(first)
    }

    private fun runCapturingMdc(request: MockHttpServletRequest, response: MockHttpServletResponse): String? {
        var seen: String? = null
        val chain = FilterChain { _, _ -> seen = MDC.get(RequestIdFilter.MDC_KEY) }
        filter.doFilter(request, response, chain)
        return seen
    }
}
