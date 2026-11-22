package com.peakda.server.domain.auth.oauth.handler

import com.peakda.server.domain.auth.app.application.AppLoginProperties
import com.peakda.server.domain.auth.oauth.model.OAuth2ClientKind
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.authentication.DisabledException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerExceptionResolver

@Component
class OAuth2AuthenticationFailureHandler(
    @param:Qualifier("handlerExceptionResolver")
    private val resolver: HandlerExceptionResolver,
    private val appLoginProperties: AppLoginProperties,
) : AuthenticationFailureHandler {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException
    ) {
        log.error("OAuth2 authentication failed: ${exception.message}", exception)

        when (OAuth2ClientKind.ofState(request.getParameter(OAuth2ParameterNames.STATE))) {
            // 앱은 Custom Tab 안에서 JSON 에러를 받으면 되돌아올 길이 없다. 딥링크로 실패를 알린다.
            OAuth2ClientKind.APP -> AppLoginRedirect.send(
                response = response,
                redirectUri = appLoginProperties.redirectUri,
                parameter = AppLoginRedirect.ERROR,
                value = errorOf(exception),
            )
            // 백오피스도 JSON 에러 화면에 멈추지 않도록 로그인 화면으로 돌려보내 사유를 보여준다.
            OAuth2ClientKind.ADMIN -> AdminLoginRedirect.sendError(response, errorOf(exception))
            OAuth2ClientKind.WEB -> resolver.resolveException(request, response, null, exception)
        }
    }

    private fun errorOf(exception: AuthenticationException): String =
        if (exception is DisabledException) USER_DISABLED else LOGIN_FAILED

    companion object {
        private const val USER_DISABLED = "USER_DISABLED"
        private const val LOGIN_FAILED = "LOGIN_FAILED"
    }
}
