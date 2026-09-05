package com.peakda.server.domain.auth.oauth.handler

import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.web.util.UriComponentsBuilder

/**
 * 앱 딥링크로 되돌려 보낸다.
 * `sendRedirect` 대신 Location 을 직접 쓰는 이유는, 서블릿 컨테이너가 커스텀 스킴을
 * 상대 경로로 오해해 절대 URL 로 바꾸려 드는 일을 피하기 위해서다.
 */
internal object AppLoginRedirect {

    const val CODE = "code"
    const val ERROR = "error"

    fun send(response: HttpServletResponse, redirectUri: String, parameter: String, value: String) {
        val location = UriComponentsBuilder.fromUriString(redirectUri)
            .queryParam(parameter, value)
            .build()
            .encode()
            .toUriString()
        response.status = HttpServletResponse.SC_FOUND
        response.setHeader(HttpHeaders.LOCATION, location)
    }
}
