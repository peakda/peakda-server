package com.peakda.server.common.storage

import org.springframework.stereotype.Component

/**
 * 이미지 참조 필드에는 외부 URL 과 우리 버킷에 업로드된 객체 key 가 섞여 들어올 수 있다.
 *
 * 외부 URL 은 그대로 노출, 내부 key 는 [resolveKey] 로 변환해서 응답한다.
 */
@Component
class ObjectKeyUrlResolver(
    private val objectStorage: ObjectStorage,
    private val properties: StorageProperties,
) {
    fun resolve(value: String?): String? {
        if (value.isNullOrBlank()) return null
        if (isExternalUrl(value)) return value
        return resolveKey(value)
    }

    /**
     * 우리 버킷 key 를 조회용 URL 로 바꾼다.
     *
     * 공개 base URL 이 설정돼 있으면 만료 없는 고정 URL 을 쓴다. 서명이 매번 달라지지 않아야
     * CDN 이 같은 이미지를 같은 캐시 키로 인식한다. 설정이 없으면 presigned URL 로 폴백한다.
     */
    fun resolveKey(key: String): String {
        val base = properties.publicBaseUrl.trimEnd('/')
        if (base.isBlank()) return objectStorage.presignedGetUrl(key)
        return "$base/${key.trimStart('/')}"
    }

    private fun isExternalUrl(value: String): Boolean =
        value.startsWith("http://") || value.startsWith("https://")
}
