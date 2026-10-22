package com.peakda.server.domain.launchalert.application

/** 메일 버튼에 거는 스토어 주소 규칙. 메일 HTML 에 그대로 들어가므로 따옴표·꺾쇠가 섞이지 않는 문자만 받는다. */
object LaunchAlertStoreUrl {
    const val PATTERN = "^\\s*https://apps\\.apple\\.com/[A-Za-z0-9/._~%?=&-]+\\s*$"

    private val regex = Regex(PATTERN)

    fun isValid(value: String): Boolean = regex.matches(value)
}
