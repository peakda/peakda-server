package com.peakda.server.infrastructure.external.kto

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "external.kto")
data class KtoProperties(
    val serviceKey: String = "",
    val baseUrl: BaseUrl = BaseUrl(),
) {
    data class BaseUrl(
        val korService: String = "",
        val tatsCnctr: String = "",
        val dataLab: String = "",
        val photo: String = "",
        val durunubi: String = "",
    )
}
