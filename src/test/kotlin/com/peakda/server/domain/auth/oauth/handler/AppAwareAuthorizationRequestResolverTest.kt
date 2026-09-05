package com.peakda.server.domain.auth.oauth.handler

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.web.util.UriUtils
import java.nio.charset.StandardCharsets

class AppAwareAuthorizationRequestResolverTest {

    private val resolver = AppAwareAuthorizationRequestResolver(
        InMemoryClientRegistrationRepository(
            ClientRegistration.withRegistrationId("kakao")
                .clientId("client-id")
                .clientSecret("client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("profile_nickname")
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .tokenUri("https://kauth.kakao.com/oauth/token")
                .userInfoUri("https://kapi.kakao.com/v2/user/me")
                .userNameAttributeName("id")
                .build(),
        ),
    )

    @Test
    fun `앱에서 시작하면 state 에 표식이 붙는다`() {
        val authorizationRequest = requireNotNull(resolver.resolve(authorizationRequest(client = "app")))

        assertThat(authorizationRequest.state).endsWith(".app")
    }

    @Test
    fun `state 를 바꾸면 인가 URL 도 같은 state 로 다시 만들어진다`() {
        val authorizationRequest = requireNotNull(resolver.resolve(authorizationRequest(client = "app")))

        assertThat(authorizationRequest.authorizationRequestUri)
            .contains("state=${encodedState(authorizationRequest.state)}")
    }

    @Test
    fun `웹에서 시작하면 state 를 건드리지 않는다`() {
        val authorizationRequest = requireNotNull(resolver.resolve(authorizationRequest(client = null)))

        assertThat(authorizationRequest.state).doesNotEndWith(".app")
        assertThat(authorizationRequest.authorizationRequestUri)
            .contains("state=${encodedState(authorizationRequest.state)}")
    }

    @Test
    fun `인가 요청 경로가 아니면 아무것도 만들지 않는다`() {
        val request = MockHttpServletRequest("GET", "/api/spots").apply { servletPath = "/api/spots" }

        assertThat(resolver.resolve(request)).isNull()
    }

    /** state 는 Base64 라 `=` 가 들어갈 수 있고, 인가 URL 에서는 퍼센트 인코딩된다. */
    private fun encodedState(state: String): String = UriUtils.encodeQueryParam(state, StandardCharsets.UTF_8)

    private fun authorizationRequest(client: String?): MockHttpServletRequest =
        MockHttpServletRequest("GET", "/oauth2/authorization/kakao").apply {
            servletPath = "/oauth2/authorization/kakao"
            client?.let { addParameter("client", it) }
        }
}
