package com.peakda.server.common.security.jwt

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletRequest

class BearerTokensTest {

    @Test
    fun `Bearer 토큰을 읽는다`() {
        assertThat(BearerTokens.from(withAuthorization("Bearer access-token"))).isEqualTo("access-token")
    }

    @Test
    fun `스킴 대소문자는 가리지 않는다`() {
        assertThat(BearerTokens.from(withAuthorization("bearer access-token"))).isEqualTo("access-token")
    }

    @Test
    fun `Bearer 가 아닌 스킴은 읽지 않는다`() {
        assertThat(BearerTokens.from(withAuthorization("Basic access-token"))).isNull()
    }

    @Test
    fun `헤더가 없거나 토큰이 비어 있으면 null 이다`() {
        assertThat(BearerTokens.from(MockHttpServletRequest())).isNull()
        assertThat(BearerTokens.from(withAuthorization("Bearer   "))).isNull()
    }

    private fun withAuthorization(value: String): MockHttpServletRequest =
        MockHttpServletRequest().apply { addHeader(HttpHeaders.AUTHORIZATION, value) }
}
