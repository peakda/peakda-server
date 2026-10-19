package com.peakda.server.domain.auth.session.entity

/** 로그인 경로. 앱은 Bearer, 웹은 쿠키로 토큰을 받고 리프레시 토큰 유효기간이 다르다. */
enum class SessionClientType {
    APP,
    WEB,
}
