package com.peakda.server.domain.auth.app.presentation.response

enum class AppAuthStatus {
    /** 가입까지 끝난 사용자. 액세스·리프레시 토큰이 함께 온다. */
    AUTHENTICATED,

    /** 소셜 인증은 끝났지만 회원가입이 남았다. 가입 세션 토큰이 함께 온다. */
    SIGNUP_REQUIRED,
}
