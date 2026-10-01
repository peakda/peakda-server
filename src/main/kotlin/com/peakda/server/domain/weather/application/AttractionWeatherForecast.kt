package com.peakda.server.domain.weather.application

data class AttractionWeatherForecast(
    val daily: List<DailyWeather>,
    val rainWindows: List<RainWindow>,
)
