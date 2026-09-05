package com.peakda.server.infrastructure.external.kma

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "external.kma")
data class KmaProperties(
    val serviceKey: String = "",
    val baseUrl: BaseUrl = BaseUrl(),
) {
    data class BaseUrl(
        val vilageFcst: String = "",
        val midFcst: String = "",
        val asosDaly: String = "",
        val flowerObservation: String = "",
    )
}
