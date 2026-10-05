package com.peakda.server.domain.user.entity

/**
 * 회원가입을 마친 클라이언트. 백오피스에서 웹·앱 유입을 구분하는 데 쓴다.
 *
 * 가입 완료 요청이 가입 세션 토큰을 Bearer 헤더로 보내면 [APP], signup-token 쿠키로 보내면 [WEB] 이다.
 * 이 구분을 저장하기 전에 가입한 계정은 [UNKNOWN] 으로 남는다.
 */
enum class SignupChannel {
    WEB,
    APP,
    UNKNOWN,
}
