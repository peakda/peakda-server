package com.peakda.server.common.openapi

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.openapi")
data class OpenApiProperties(
    val servers: Servers,
) {
    data class Servers(
        val local: String,
        val dev: String,
    )
}
