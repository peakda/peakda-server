package com.peakda.server.domain.weather.repository

/** 단기예보 격자와 그 격자에 속한 명소 수. */
data class ForecastGrid(
    val gridX: Int,
    val gridY: Int,
    val attractionCount: Long,
)
