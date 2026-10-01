package com.peakda.server.domain.weather.application

import java.time.LocalDate

/** 명소의 하루 날씨 요약. 3일 이내는 단기예보, 그 이후는 중기예보 기준이다. */
data class DailyWeather(
    val date: LocalDate,
    val sky: WeatherSky?,
    /** 낮 시간대 최대 강수확률(%). */
    val precipitationProbability: Int?,
    val minTemperature: Double?,
    val maxTemperature: Double?,
)
