package com.peakda.server.domain.location.entity

/**
 * 위치정보 취득경로. 요청의 `User-Agent` 로 클라이언트 종류를 판별한다.
 *
 * 앱이 기본 UA 를 쓰면 판별이 어긋날 수 있어, 어느 쪽으로도 단정할 수 없으면 [UNKNOWN] 으로 남긴다.
 */
enum class LocationAccessChannel {
    ANDROID,
    IOS,
    WEB,
    UNKNOWN,
    ;

    companion object {
        private val IOS_MARKERS = listOf("iphone", "ipad", "ipod", "ios", "cfnetwork", "darwin")
        private val WEB_MARKERS = listOf("mozilla", "chrome", "safari", "firefox", "edge")

        fun from(userAgent: String?): LocationAccessChannel {
            val normalized = userAgent?.lowercase()?.takeIf { it.isNotBlank() } ?: return UNKNOWN
            return when {
                normalized.contains("android") -> ANDROID
                IOS_MARKERS.any { normalized.contains(it) } -> IOS
                WEB_MARKERS.any { normalized.contains(it) } -> WEB
                else -> UNKNOWN
            }
        }
    }
}
