package com.peakda.server.common.storage

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.storage")
data class StorageProperties(
    val bucket: String,
    val endpoint: String = "https://s3.amazonaws.com",
    val region: String = "auto",
    val accessKey: String = "",
    val secretKey: String = "",
    val pathStyleAccess: Boolean = true,
    val presignedUrlTtlSeconds: Long = 604800,
    /**
     * 버킷 앞단 CDN 의 공개 base URL (예: `https://cdn.peakda.com`).
     *
     * 값이 있으면 이미지 URL 은 `{publicBaseUrl}/{key}` 로 고정되어 만료가 없고 CDN 캐시 키도 안정된다.
     * 비어 있으면 presigned URL 로 폴백한다.
     */
    val publicBaseUrl: String = "",
)
