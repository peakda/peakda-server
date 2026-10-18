package com.peakda.server.domain.auth.session.application

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * @property maxDevicesPerUser 사용자당 동시에 로그인해 둘 수 있는 기기 수. 넘으면 가장 오래 안 쓴 기기부터 해제한다.
 * @property rotationGrace 회전 직후 직전 리프레시 토큰을 받아 주는 시간. 앱이 재발급을 동시에 여러 번 보내거나
 *   응답을 못 받고 다시 보낸 경우 같은 결과를 돌려준다.
 */
@ConfigurationProperties(prefix = "app.auth.session")
data class AuthSessionProperties(
    val maxDevicesPerUser: Int = 5,
    val rotationGrace: Duration = Duration.ofSeconds(60),
)
