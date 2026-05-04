package com.peakda.server.infrastructure.external.pubdata

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "external.pubdata")
data class PubdataProperties(
    val festival: Festival = Festival(),
) {
    data class Festival(
        val serviceKey: String = "",
        val baseUrl: String = "",
    )
}
