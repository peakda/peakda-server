package com.peakda.server.domain.auth.signup.application

import com.peakda.server.common.image.ImageVariant
import com.peakda.server.domain.user.application.ProfileImagePolicy

object SignupProfileImagePolicy {

    private const val TEMP_PREFIX_FORMAT = "temp/signup/%d/"
    private const val TEMP_MARKER = "temp/signup/"

    fun prefix(sessionId: Long): String = TEMP_PREFIX_FORMAT.format(sessionId)

    fun keyOf(sessionId: Long, variant: ImageVariant): String =
        prefix(sessionId) + "${variant.name}.${variant.format.extension}"

    /**
     * 주어진 값이 해당 가입 세션의 임시 업로드 영역에 속한 key 인지 판단.
     * 외부 OAuth 가 준 URL 은 우리 버킷의 key 가 아니므로 false.
     */
    fun isManaged(sessionId: Long, value: String): Boolean =
        ProfileImagePolicy.VARIANTS.any { value == keyOf(sessionId, it) }

    /**
     * 가입 완료 시 받은 값이 우리 임시 버킷의 key 라면 어떤 sessionId 의 것인지 추출.
     * 외부 URL 이거나 형식이 맞지 않으면 null.
     */
    fun extractSessionId(value: String): Long? {
        if (!value.startsWith(TEMP_MARKER)) return null
        val tail = value.substring(TEMP_MARKER.length)
        val slash = tail.indexOf('/')
        if (slash <= 0) return null
        return tail.substring(0, slash).toLongOrNull()
    }
}
