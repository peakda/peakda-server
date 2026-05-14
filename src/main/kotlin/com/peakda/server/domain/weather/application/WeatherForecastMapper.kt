package com.peakda.server.domain.weather.application

import com.peakda.server.domain.weather.entity.WeatherMidForecast
import com.peakda.server.domain.weather.entity.WeatherShortForecast
import com.peakda.server.infrastructure.external.kma.midfcst.response.MidLandFcstItem
import com.peakda.server.infrastructure.external.kma.midfcst.response.MidTaItem
import com.peakda.server.infrastructure.external.kma.vilagefcst.response.VilageFcstItem

fun WeatherMidForecast.applyLand(item: MidLandFcstItem) {
    weatherDay3Am = item.wf3Am.ifBlank { weatherDay3Am }
    weatherDay3Pm = item.wf3Pm.ifBlank { weatherDay3Pm }
    weatherDay4Am = item.wf4Am.ifBlank { weatherDay4Am }
    weatherDay4Pm = item.wf4Pm.ifBlank { weatherDay4Pm }
    weatherDay5Am = item.wf5Am.ifBlank { weatherDay5Am }
    weatherDay5Pm = item.wf5Pm.ifBlank { weatherDay5Pm }
    weatherDay6Am = item.wf6Am.ifBlank { weatherDay6Am }
    weatherDay6Pm = item.wf6Pm.ifBlank { weatherDay6Pm }
    weatherDay7Am = item.wf7Am.ifBlank { weatherDay7Am }
    weatherDay7Pm = item.wf7Pm.ifBlank { weatherDay7Pm }
    weatherDay8 = item.wf8.ifBlank { weatherDay8 }
    weatherDay9 = item.wf9.ifBlank { weatherDay9 }
    weatherDay10 = item.wf10.ifBlank { weatherDay10 }
    rainProbabilityDay3Am = item.rnSt3Am
    rainProbabilityDay3Pm = item.rnSt3Pm
    rainProbabilityDay4Am = item.rnSt4Am
    rainProbabilityDay4Pm = item.rnSt4Pm
    rainProbabilityDay5Am = item.rnSt5Am
    rainProbabilityDay5Pm = item.rnSt5Pm
    rainProbabilityDay6Am = item.rnSt6Am
    rainProbabilityDay6Pm = item.rnSt6Pm
    rainProbabilityDay7Am = item.rnSt7Am
    rainProbabilityDay7Pm = item.rnSt7Pm
    rainProbabilityDay8 = item.rnSt8
    rainProbabilityDay9 = item.rnSt9
    rainProbabilityDay10 = item.rnSt10
}

fun WeatherMidForecast.applyTa(item: MidTaItem) {
    temperatureMinDay3 = item.taMin3
    temperatureMaxDay3 = item.taMax3
    temperatureMinDay4 = item.taMin4
    temperatureMaxDay4 = item.taMax4
    temperatureMinDay5 = item.taMin5
    temperatureMaxDay5 = item.taMax5
    temperatureMinDay6 = item.taMin6
    temperatureMaxDay6 = item.taMax6
    temperatureMinDay7 = item.taMin7
    temperatureMaxDay7 = item.taMax7
    temperatureMinDay8 = item.taMin8
    temperatureMaxDay8 = item.taMax8
    temperatureMinDay9 = item.taMin9
    temperatureMaxDay9 = item.taMax9
    temperatureMinDay10 = item.taMin10
    temperatureMaxDay10 = item.taMax10
}

fun VilageFcstItem.toShortForecast(): WeatherShortForecast = WeatherShortForecast(
    gridX = nx,
    gridY = ny,
    announceDate = baseDate,
    announceTime = baseTime,
    forecastDate = fcstDate,
    forecastTime = fcstTime,
    forecastCategory = category,
    forecastValue = fcstValue,
)

fun WeatherShortForecast.applyUpdate(item: VilageFcstItem) {
    announceDate = item.baseDate
    announceTime = item.baseTime
    forecastValue = item.fcstValue
}
