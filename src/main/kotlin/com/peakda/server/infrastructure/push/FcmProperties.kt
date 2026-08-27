package com.peakda.server.infrastructure.push

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.push")
data class FcmProperties(
    val enabled: Boolean = false,
    val projectId: String? = null,
    val serviceAccountLocation: String? = null,
    val serviceAccountBase64: String? = null,
)
