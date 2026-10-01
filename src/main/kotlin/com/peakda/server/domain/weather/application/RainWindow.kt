package com.peakda.server.domain.weather.application

import java.time.LocalDateTime

/** 단기예보에서 강수가 이어지는 시간 구간 ([start] 포함, [end] 제외). */
data class RainWindow(
    val start: LocalDateTime,
    val end: LocalDateTime,
    val sky: WeatherSky,
)
