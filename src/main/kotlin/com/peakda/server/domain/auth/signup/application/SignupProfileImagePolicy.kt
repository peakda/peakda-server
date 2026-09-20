package com.peakda.server.domain.auth.signup.application

object SignupProfileImagePolicy {

    private const val TEMP_ROOT = "temp/signup"

    /** 가입 세션의 임시 업로드 prefix. 본 프로필과 같은 이유로 업로드마다 임의 문자열을 붙인다. */
    fun prefixOf(sessionId: Long, uuid: String): String = "$TEMP_ROOT/$sessionId/$uuid"

    /**
     * 주어진 값이 해당 가입 세션의 임시 업로드 영역에 속한 key 인지 판단.
     * 외부 OAuth 가 준 URL 은 우리 버킷의 key 가 아니므로 false.
     */
    fun isManaged(sessionId: Long, value: String): Boolean = value.startsWith("$TEMP_ROOT/$sessionId/")

    /**
     * 가입 완료 시 받은 값이 우리 임시 버킷의 key 라면 어떤 sessionId 의 것인지 추출.
     * 외부 URL 이거나 형식이 맞지 않으면 null.
     */
    fun extractSessionId(value: String): Long? {
        val marker = "$TEMP_ROOT/"
        if (!value.startsWith(marker)) return null
        val tail = value.substring(marker.length)
        val slash = tail.indexOf('/')
        if (slash <= 0) return null
        return tail.substring(0, slash).toLongOrNull()
    }
}
