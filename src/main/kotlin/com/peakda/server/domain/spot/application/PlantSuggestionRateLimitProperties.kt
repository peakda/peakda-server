package com.peakda.server.domain.spot.application

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "app.spot.plant-suggestion.rate-limit")
data class PlantSuggestionRateLimitProperties(
    val maxPerWindow: Int = 5,
    val window: Duration = Duration.ofHours(24),
)
