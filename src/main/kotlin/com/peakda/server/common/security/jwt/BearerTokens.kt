package com.peakda.server.common.security.jwt

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders

/**
 * Authorization 헤더에서 Bearer 토큰을 읽는다.
 *
 * 웹은 HttpOnly 쿠키로 인증하고 앱은 이 헤더를 쓴다. 앱은 Custom Tab 으로 소셜 인증을 하는데,
 * 거기서 받은 쿠키는 브라우저 저장소에 남아 WebView 로 넘어오지 않기 때문이다.
 */
object BearerTokens {

    private const val PREFIX = "Bearer "

    fun from(request: HttpServletRequest): String? =
        request.getHeader(HttpHeaders.AUTHORIZATION)
            ?.takeIf { it.startsWith(PREFIX, ignoreCase = true) }
            ?.substring(PREFIX.length)
            ?.trim()
            ?.ifBlank { null }
}
