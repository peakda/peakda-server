package com.peakda.server.domain.auth.app.application

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "app.oauth2.app")
data class AppLoginProperties(
    /** 앱이 소셜 인증을 마치고 돌아올 딥링크. `code` 또는 `error` 쿼리가 붙는다. */
    val redirectUri: String = "peakda://auth/callback",

    /** 일회성 코드 유효 시간. 앱이 딥링크를 받는 즉시 교환하므로 짧게 잡는다. */
    val codeTtl: Duration = Duration.ofSeconds(60),
)
