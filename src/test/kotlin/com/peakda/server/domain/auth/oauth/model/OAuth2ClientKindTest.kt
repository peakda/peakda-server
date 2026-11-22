package com.peakda.server.domain.auth.oauth.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest

class OAuth2ClientKindTest {

    @Test
    fun `client=app 파라미터로 시작한 인가 요청은 앱이다`() {
        val request = MockHttpServletRequest().apply { addParameter("client", "app") }

        assertThat(OAuth2ClientKind.ofRequest(request)).isEqualTo(OAuth2ClientKind.APP)
    }

    @Test
    fun `파라미터가 없거나 다른 값이면 웹이다`() {
        assertThat(OAuth2ClientKind.ofRequest(MockHttpServletRequest())).isEqualTo(OAuth2ClientKind.WEB)
        assertThat(
            OAuth2ClientKind.ofRequest(MockHttpServletRequest().apply { addParameter("client", "web") }),
        ).isEqualTo(OAuth2ClientKind.WEB)
    }

    @Test
    fun `앱 표식을 붙인 state 는 콜백에서 앱으로 되읽힌다`() {
        val marked = OAuth2ClientKind.markState("random-state", OAuth2ClientKind.APP)

        assertThat(marked).isNotEqualTo("random-state")
        assertThat(OAuth2ClientKind.ofState(marked)).isEqualTo(OAuth2ClientKind.APP)
    }

    @Test
    fun `웹은 state 를 건드리지 않는다`() {
        val marked = OAuth2ClientKind.markState("random-state", OAuth2ClientKind.WEB)

        assertThat(marked).isEqualTo("random-state")
        assertThat(OAuth2ClientKind.ofState(marked)).isEqualTo(OAuth2ClientKind.WEB)
    }

    @Test
    fun `client=admin 파라미터로 시작한 인가 요청은 백오피스다`() {
        val request = MockHttpServletRequest().apply { addParameter("client", "ADMIN") }

        assertThat(OAuth2ClientKind.ofRequest(request)).isEqualTo(OAuth2ClientKind.ADMIN)
    }

    @Test
    fun `백오피스 표식을 붙인 state 는 콜백에서 백오피스로 되읽힌다`() {
        val marked = OAuth2ClientKind.markState("random-state", OAuth2ClientKind.ADMIN)

        assertThat(marked).isNotEqualTo("random-state")
        assertThat(OAuth2ClientKind.ofState(marked)).isEqualTo(OAuth2ClientKind.ADMIN)
        assertThat(OAuth2ClientKind.ofState(OAuth2ClientKind.markState("random-state", OAuth2ClientKind.APP)))
            .isEqualTo(OAuth2ClientKind.APP)
    }

    @Test
    fun `점으로 시작하는 표식과 정확히 일치할 때만 앱·백오피스로 본다`() {
        // 스프링 기본 state 는 Base64URL(패딩 포함)이라 점이 들어가지 않는다.
        assertThat(OAuth2ClientKind.ofState("Q2xhdWRlLXN0YXRlLXJhbmRvbS0zMi1ieXRlcy1hYmM=")).isEqualTo(OAuth2ClientKind.WEB)
        assertThat(OAuth2ClientKind.ofState("random-stateadmin")).isEqualTo(OAuth2ClientKind.WEB)
        assertThat(OAuth2ClientKind.ofState("random-stateapp")).isEqualTo(OAuth2ClientKind.WEB)
    }

    @Test
    fun `빈 client 파라미터는 웹이다`() {
        val request = MockHttpServletRequest().apply { addParameter("client", "") }

        assertThat(OAuth2ClientKind.ofRequest(request)).isEqualTo(OAuth2ClientKind.WEB)
    }

    @Test
    fun `state 가 없으면 웹으로 본다`() {
        assertThat(OAuth2ClientKind.ofState(null)).isEqualTo(OAuth2ClientKind.WEB)
    }
}
