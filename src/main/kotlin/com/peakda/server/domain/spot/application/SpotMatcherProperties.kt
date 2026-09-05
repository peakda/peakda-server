package com.peakda.server.domain.spot.application

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.spot.matcher")
data class SpotMatcherProperties(
    val radiusMeters: Double = 50.0,
)
