package com.peakda.server.domain.auth.session.application

/**
 * 웹 세션의 표시 이름을 User-Agent 에서 `브라우저 (OS)` 형태로 만든다. 예: `Chrome (macOS)`.
 * 기기 목록에서 알아보기 위한 이름이라 주요 브라우저·OS 만 구분한다.
 */
object WebDeviceNames {

    private val BROWSERS = listOf(
        "Edg/" to "Edge",
        "Whale/" to "Whale",
        "SamsungBrowser/" to "Samsung Internet",
        "KAKAOTALK" to "카카오톡",
        "NAVER(" to "네이버",
        "Firefox/" to "Firefox",
        "Chrome/" to "Chrome",
        "CriOS/" to "Chrome",
        "Safari/" to "Safari",
    )

    private val OPERATING_SYSTEMS = listOf(
        "iPhone" to "iOS",
        "iPad" to "iPadOS",
        "Android" to "Android",
        "Windows" to "Windows",
        "Mac OS X" to "macOS",
        "Linux" to "Linux",
    )

    fun of(userAgent: String?): String {
        if (userAgent.isNullOrBlank()) return "웹 브라우저"
        val browser = BROWSERS.firstOrNull { (token, _) -> userAgent.contains(token) }?.second ?: "웹 브라우저"
        val os = OPERATING_SYSTEMS.firstOrNull { (token, _) -> userAgent.contains(token) }?.second
        return if (os == null) browser else "$browser ($os)"
    }
}
