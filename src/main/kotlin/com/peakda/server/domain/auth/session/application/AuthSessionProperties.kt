package com.peakda.server.domain.auth.session.application

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * @property maxDevicesPerUser 사용자당 동시에 로그인해 둘 수 있는 기기 수. 넘으면 가장 오래 안 쓴 기기부터 해제한다.
 * @property rotationGrace 회전 결과를 남겨 두는 시간. 이 안에 같은 리프레시 토큰으로 다시 오면(동시 재발급·응답 유실)
 *   같은 토큰 쌍을 돌려준다. 지난 뒤에 오면 새로 회전한다.
 */
@ConfigurationProperties(prefix = "app.auth.session")
data class AuthSessionProperties(
    val maxDevicesPerUser: Int = 5,
    val rotationGrace: Duration = Duration.ofSeconds(60),
)
