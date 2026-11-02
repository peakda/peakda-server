package com.peakda.server.domain.auth.apple.application

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * iOS 앱의 Sign in with Apple.
 *
 * 개인키(.p8)는 base64 가 있으면 그것을, 없으면 파일 경로를 읽는다(FCM 서비스 계정과 같은 방식).
 * 운영은 compose `.env` 에 여러 줄 값을 넣을 수 없어 base64 한 줄로, 로컬은 파일 경로로 넣는다.
 */
@ConfigurationProperties("app.apple")
data class AppleProperties(
    val enabled: Boolean = false,
    val teamId: String? = null,
    val keyId: String? = null,
    /** identity token 의 aud 이자 애플 API 의 client_id. 네이티브 로그인에서는 앱 번들 ID 다. */
    val bundleId: String = "com.peakda.app",
    val privateKeyBase64: String? = null,
    val privateKeyLocation: String? = null,
    val connectTimeout: Duration = Duration.ofSeconds(3),
    val readTimeout: Duration = Duration.ofSeconds(5),
) {
    init {
        // 켜 두고 키를 빠뜨리면 첫 로그인에서야 드러난다. 기동 단계에서 막는다.
        if (enabled) {
            require(!teamId.isNullOrBlank()) { "app.apple.enabled 가 true 이면 APPLE_TEAM_ID 가 필요하다" }
            require(!keyId.isNullOrBlank()) { "app.apple.enabled 가 true 이면 APPLE_KEY_ID 가 필요하다" }
            require(!privateKeyBase64.isNullOrBlank() || !privateKeyLocation.isNullOrBlank()) {
                "app.apple.enabled 가 true 이면 APPLE_PRIVATE_KEY_BASE64 또는 APPLE_PRIVATE_KEY_LOCATION 이 필요하다"
            }
        }
    }
}
