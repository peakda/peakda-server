package com.peakda.server.domain.auth.oauth.handler

import com.peakda.server.domain.auth.oauth.model.OAuth2ClientKind
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.stereotype.Component

/**
 * 앱에서 시작한 인가 요청의 state 에 표식을 남겨, 콜백에서 앱임을 알아볼 수 있게 한다.
 * state 를 바꾸면 인가 URL 도 함께 다시 만들어지므로 저장본과 콜백의 state 는 그대로 일치한다.
 */
@Component
class AppAwareAuthorizationRequestResolver(
    clientRegistrationRepository: ClientRegistrationRepository,
) : OAuth2AuthorizationRequestResolver {

    private val delegate = DefaultOAuth2AuthorizationRequestResolver(
        clientRegistrationRepository,
        OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI,
    )

    override fun resolve(request: HttpServletRequest): OAuth2AuthorizationRequest? =
        markClient(delegate.resolve(request), request)

    override fun resolve(request: HttpServletRequest, clientRegistrationId: String): OAuth2AuthorizationRequest? =
        markClient(delegate.resolve(request, clientRegistrationId), request)

    private fun markClient(
        authorizationRequest: OAuth2AuthorizationRequest?,
        request: HttpServletRequest,
    ): OAuth2AuthorizationRequest? {
        if (authorizationRequest == null) return null
        val kind = OAuth2ClientKind.ofRequest(request)
        if (kind == OAuth2ClientKind.WEB) return authorizationRequest

        return OAuth2AuthorizationRequest.from(authorizationRequest)
            .state(OAuth2ClientKind.markState(authorizationRequest.state, kind))
            .build()
    }
}
