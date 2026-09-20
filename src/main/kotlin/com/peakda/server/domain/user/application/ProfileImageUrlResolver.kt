package com.peakda.server.domain.user.application

import com.peakda.server.common.storage.ObjectKeyUrlResolver
import org.springframework.stereotype.Component

/**
 * 프로필 이미지 참조 값을 화면 용도에 맞는 URL 로 바꾼다.
 *
 * 값은 우리 버킷 key 이거나 OAuth 제공자가 준 외부 URL 이다. 외부 URL 은 사이즈 선택지가 없으므로
 * 어느 용도에서든 그대로 노출한다.
 */
@Component
class ProfileImageUrlResolver(
    private val objectKeyUrlResolver: ObjectKeyUrlResolver,
) {
    /** 대표(512) URL. 프로필 상세 화면이 쓴다. */
    fun mainUrl(value: String?): String? = objectKeyUrlResolver.resolve(value)

    /** 아바타(128) URL. 피드 카드·목록처럼 작게 쓰는 자리에 쓴다. */
    fun thumbnailUrl(value: String?): String? {
        if (value.isNullOrBlank()) return null
        if (!objectKeyUrlResolver.isObjectKey(value)) return value
        val thumbnail = variantOf(ProfileImagePolicy.THUMBNAIL_VARIANT) ?: return mainUrl(value)
        return objectKeyUrlResolver.resolveKey(ProfileImagePolicy.variantKeyOf(value, thumbnail))
    }

    /** 사이즈 variant 별 URL. 외부 URL 이면 고를 사이즈가 없으므로 빈 맵. */
    fun variantUrls(value: String?): Map<String, String> {
        if (value.isNullOrBlank()) return emptyMap()
        if (!objectKeyUrlResolver.isObjectKey(value)) return emptyMap()
        return ProfileImagePolicy.VARIANTS.associate { variant ->
            variant.name to objectKeyUrlResolver.resolveKey(ProfileImagePolicy.variantKeyOf(value, variant))
        }
    }

    private fun variantOf(name: String) = ProfileImagePolicy.VARIANTS.firstOrNull { it.name == name }
}
