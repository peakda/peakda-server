package com.peakda.server.domain.auth.oauth.handler

import jakarta.servlet.http.HttpServletResponse

/**
 * 백오피스에서 시작한 로그인을 백오피스 첫 화면으로 되돌려 보낸다.
 * 같은 서버의 고정 경로만 쓰므로 외부 주소로 새어 나갈 여지가 없다.
 * 로그인 전에 보던 화면은 백오피스가 브라우저에 기억해 두었다가 스스로 복원한다.
 *
 * `/admin/` 는 `/admin/index.html` 로 한 번 더 리다이렉트되며 쿼리가 버려지므로 index.html 로 바로 보낸다.
 */
internal object AdminLoginRedirect {

    private const val HOME = "/admin/index.html"

    /** 가입 절차는 사용자용 웹·앱에만 있다. 백오피스에서는 가입된 관리자 계정만 로그인할 수 있다. */
    const val SIGNUP_REQUIRED = "SIGNUP_REQUIRED"

    fun sendHome(response: HttpServletResponse) {
        response.sendRedirect(HOME)
    }

    fun sendError(response: HttpServletResponse, error: String) {
        response.sendRedirect("$HOME?error=$error")
    }
}
