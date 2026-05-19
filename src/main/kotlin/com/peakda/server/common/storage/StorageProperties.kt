package com.peakda.server.common.storage

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.storage")
data class StorageProperties(
    val bucket: String,
    val endpoint: String,
    val region: String = "auto",
    val accessKey: String,
    val secretKey: String,
    val publicBaseUrl: String,
    val pathStyleAccess: Boolean = true,
)
