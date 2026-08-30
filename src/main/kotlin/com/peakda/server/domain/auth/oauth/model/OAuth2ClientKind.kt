package com.peakda.server.domain.auth.oauth.model

import jakarta.servlet.http.HttpServletRequest

/**
 * 소셜 로그인을 시작한 클라이언트.
 *
 * 인가 요청의 `state` 에 접미사로 실려 콜백까지 따라온다. 세션 속성을 쓰지 않는 이유는,
 * 저장된 인가 요청이 콜백 처리 도중 세션에서 제거된 뒤에야 성공·실패 핸들러가 실행되기 때문이다.
 * state 는 스프링이 저장본과 대조만 하므로 접미사를 붙여도 검증에 영향이 없다.
 */
enum class OAuth2ClientKind {
    WEB,
    APP,
    ;

    companion object {
        private const val CLIENT_PARAMETER = "client"
        private const val APP_CLIENT = "app"
        private const val APP_STATE_SUFFIX = ".app"

        /** 인가 요청 진입 시점 판별. `/oauth2/authorization/{provider}?client=app` 이면 앱이다. */
        fun ofRequest(request: HttpServletRequest): OAuth2ClientKind =
            if (APP_CLIENT.equals(request.getParameter(CLIENT_PARAMETER), ignoreCase = true)) APP else WEB

        /** 콜백 시점 판별. */
        fun ofState(state: String?): OAuth2ClientKind =
            if (state != null && state.endsWith(APP_STATE_SUFFIX)) APP else WEB

        fun markState(state: String, kind: OAuth2ClientKind): String =
            if (kind == APP) state + APP_STATE_SUFFIX else state
    }
}
