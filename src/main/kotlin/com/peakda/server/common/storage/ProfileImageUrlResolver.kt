package com.peakda.server.common.storage

import org.springframework.stereotype.Component

/**
 * `User.profileImageUrl` / `SignupSession.profileImageUrl` 필드에는 두 가지 값이 섞여 들어온다.
 *  - OAuth2 제공자(카카오 등) 가 준 외부 URL (`http://...`)
 *  - 우리 버킷에 업로드된 객체의 key (`profile-images/1/main.jpg`, `temp/signup/2/main.jpg`)
 *
 * 외부 URL 은 그대로 노출, 내부 key 는 presigned URL 로 변환해서 응답한다.
 */
@Component
class ProfileImageUrlResolver(
    private val objectStorage: ObjectStorage,
) {
    fun resolve(value: String?): String? {
        if (value.isNullOrBlank()) return null
        if (isExternalUrl(value)) return value
        return objectStorage.presignedGetUrl(value)
    }

    private fun isExternalUrl(value: String): Boolean =
        value.startsWith("http://") || value.startsWith("https://")
}
