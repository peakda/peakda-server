package com.peakda.server.domain.auth.application

import com.peakda.server.domain.auth.oauth.model.KakaoOAuth2UserInfo
import com.peakda.server.domain.auth.oauth.model.GoogleOAuth2UserInfo
import com.peakda.server.domain.auth.oauth.model.NaverOAuth2UserInfo
import com.peakda.server.domain.auth.oauth.model.OAuth2LoginType
import com.peakda.server.domain.auth.oauth.model.OAuth2UserInfo
import com.peakda.server.domain.auth.signup.application.SignupSessionService
import com.peakda.server.domain.user.repository.UserRepository
import com.peakda.server.common.security.principal.OAuth2SignupPrincipal
import com.peakda.server.common.security.principal.PrincipalDetails
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CustomOAuth2UserService(
    private val userRepository: UserRepository,
    private val signupSessionService: SignupSessionService,
) : DefaultOAuth2UserService() {

    private val log = LoggerFactory.getLogger(this::class.java)

    @Transactional
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = super.loadUser(userRequest)
        val registrationId = userRequest.clientRegistration.registrationId

        log.info("OAuth2 login attempt with provider: {}", registrationId)

        val loginType = OAuth2LoginType.from(registrationId)
            ?: throw OAuth2AuthenticationException("지원하지 않는 소셜 로그인입니다: $registrationId")

        val userInfo = resolveUserInfo(loginType, oAuth2User.attributes)
        val user = userRepository.findByProviderAndProviderId(loginType, userInfo.getProviderId())

        if (user != null) {
            return PrincipalDetails(user, oAuth2User.attributes)
        }

        val signupSession = signupSessionService.createOrRefresh(loginType, userInfo)
        return OAuth2SignupPrincipal(signupSession, oAuth2User.attributes)
    }

    private fun resolveUserInfo(
        type: OAuth2LoginType,
        attributes: Map<String, Any>
    ): OAuth2UserInfo = when (type) {
        OAuth2LoginType.KAKAO -> KakaoOAuth2UserInfo(attributes)
        OAuth2LoginType.NAVER -> NaverOAuth2UserInfo(attributes)
        OAuth2LoginType.GOOGLE -> GoogleOAuth2UserInfo(attributes)
        OAuth2LoginType.APPLE ->
            throw OAuth2AuthenticationException("아직 지원하지 않는 소셜 로그인입니다: ${type.provider}")
    }

}
