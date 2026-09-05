package com.peakda.server.common.security

import com.peakda.server.domain.auth.application.CustomOAuth2UserService
import com.peakda.server.domain.auth.oauth.handler.AppAwareAuthorizationRequestResolver
import com.peakda.server.domain.auth.oauth.handler.OAuth2AuthenticationFailureHandler
import com.peakda.server.domain.auth.oauth.handler.OAuth2AuthenticationSuccessHandler
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer
import org.springframework.stereotype.Component

@Component
class OAuth2SecurityConfig(
    private val customOAuth2UserService: CustomOAuth2UserService,
    private val oAuth2AuthenticationSuccessHandler: OAuth2AuthenticationSuccessHandler,
    private val oAuth2AuthenticationFailureHandler: OAuth2AuthenticationFailureHandler,
    private val appAwareAuthorizationRequestResolver: AppAwareAuthorizationRequestResolver,
) {

    fun configure(oauth2: OAuth2LoginConfigurer<HttpSecurity>) {
        oauth2
            .authorizationEndpoint { endpoint ->
                endpoint.authorizationRequestResolver(appAwareAuthorizationRequestResolver)
            }
            .userInfoEndpoint { userInfo ->
                userInfo.userService(customOAuth2UserService)
            }
            .successHandler(oAuth2AuthenticationSuccessHandler)
            .failureHandler(oAuth2AuthenticationFailureHandler)
    }
}
