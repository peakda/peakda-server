package com.peakda.server.common.storage

import org.springframework.stereotype.Component

/**
 * 이미지 참조 필드에는 외부 URL 과 우리 버킷에 업로드된 객체 key 가 섞여 들어올 수 있다.
 *
 * 외부 URL 은 그대로 노출, 내부 key 는 presigned URL 로 변환해서 응답한다.
 */
@Component
class ObjectKeyUrlResolver(
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
