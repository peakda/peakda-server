package com.peakda.server.domain.auth.signup.application

import com.peakda.server.common.image.ImageVariant
import com.peakda.server.domain.user.application.ProfileImagePolicy

object SignupProfileImagePolicy {

    private const val TEMP_PREFIX_FORMAT = "temp/signup/%d/"

    fun prefix(sessionId: Long): String = TEMP_PREFIX_FORMAT.format(sessionId)

    fun keyOf(sessionId: Long, variant: ImageVariant): String =
        prefix(sessionId) + "${variant.name}.${variant.format.extension}"

    fun isManaged(sessionId: Long, url: String): Boolean =
        ProfileImagePolicy.VARIANTS.any { url.endsWith(keyOf(sessionId, it)) }

    /**
     * 가입 완료 시 우리 buckets URL 인지 식별하기 위한 임시 키 prefix 매칭.
     * 사용자가 보낸 URL이 어떤 sessionId 의 temp 영역인지 추출한다.
     */
    fun extractSessionId(url: String): Long? {
        val marker = "/temp/signup/"
        val idx = url.indexOf(marker)
        if (idx < 0) return null
        val tail = url.substring(idx + marker.length)
        val slash = tail.indexOf('/')
        if (slash <= 0) return null
        return tail.substring(0, slash).toLongOrNull()
    }
}
