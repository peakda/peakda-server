package com.peakda.server.infrastructure.external.common

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "external.data-go-kr")
data class DataGoKrProperties(
    val mobileApp: String = "peakda",
    val connectTimeout: Duration = Duration.ofSeconds(3),
    val readTimeout: Duration = Duration.ofSeconds(5),
)
