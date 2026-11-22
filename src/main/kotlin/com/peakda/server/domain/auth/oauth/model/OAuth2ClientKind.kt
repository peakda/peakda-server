package com.peakda.server.domain.auth.oauth.model

import jakarta.servlet.http.HttpServletRequest

/**
 * 소셜 로그인을 시작한 클라이언트.
 *
 * 인가 요청의 `state` 에 접미사로 실려 콜백까지 따라온다. 세션 속성을 쓰지 않는 이유는,
 * 저장된 인가 요청이 콜백 처리 도중 세션에서 제거된 뒤에야 성공·실패 핸들러가 실행되기 때문이다.
 * state 는 스프링이 저장본과 대조만 하므로 접미사를 붙여도 검증에 영향이 없다.
 *
 * @property clientParameter 인가 요청의 `client` 파라미터 값. 웹은 파라미터 없이 시작한다.
 * @property stateSuffix state 에 붙이는 표식. 웹은 state 를 건드리지 않는다.
 */
enum class OAuth2ClientKind(
    private val clientParameter: String?,
    private val stateSuffix: String?,
) {
    WEB(null, null),
    APP("app", ".app"),

    /** 서버가 함께 서빙하는 백오피스(`/admin`). 로그인 후 사용자용 웹이 아니라 백오피스로 돌아간다. */
    ADMIN("admin", ".admin"),
    ;

    companion object {
        private const val CLIENT_PARAMETER = "client"

        /** 인가 요청 진입 시점 판별. 예: `/oauth2/authorization/{provider}?client=app` 이면 앱이다. */
        fun ofRequest(request: HttpServletRequest): OAuth2ClientKind {
            val client = request.getParameter(CLIENT_PARAMETER) ?: return WEB
            return entries.firstOrNull { it.clientParameter.equals(client, ignoreCase = true) } ?: WEB
        }

        /** 콜백 시점 판별. */
        fun ofState(state: String?): OAuth2ClientKind {
            if (state == null) return WEB
            return entries.firstOrNull { kind -> kind.stateSuffix?.let(state::endsWith) == true } ?: WEB
        }

        fun markState(state: String, kind: OAuth2ClientKind): String =
            kind.stateSuffix?.let { state + it } ?: state
    }
}
