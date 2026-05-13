package com.peakda.server.domain.weather.application

import com.peakda.server.domain.weather.entity.WeatherMidForecast
import com.peakda.server.domain.weather.entity.WeatherShortForecast
import com.peakda.server.infrastructure.external.kma.midfcst.response.MidLandFcstItem
import com.peakda.server.infrastructure.external.kma.midfcst.response.MidTaItem
import com.peakda.server.infrastructure.external.kma.vilagefcst.response.VilageFcstItem

fun WeatherMidForecast.applyLand(item: MidLandFcstItem) {
    wf3Am = item.wf3Am.ifBlank { wf3Am }
    wf3Pm = item.wf3Pm.ifBlank { wf3Pm }
    wf4Am = item.wf4Am.ifBlank { wf4Am }
    wf4Pm = item.wf4Pm.ifBlank { wf4Pm }
    wf5Am = item.wf5Am.ifBlank { wf5Am }
    wf5Pm = item.wf5Pm.ifBlank { wf5Pm }
    wf6Am = item.wf6Am.ifBlank { wf6Am }
    wf6Pm = item.wf6Pm.ifBlank { wf6Pm }
    wf7Am = item.wf7Am.ifBlank { wf7Am }
    wf7Pm = item.wf7Pm.ifBlank { wf7Pm }
    wf8 = item.wf8.ifBlank { wf8 }
    wf9 = item.wf9.ifBlank { wf9 }
    wf10 = item.wf10.ifBlank { wf10 }
    rnSt3Am = item.rnSt3Am
    rnSt3Pm = item.rnSt3Pm
    rnSt4Am = item.rnSt4Am
    rnSt4Pm = item.rnSt4Pm
    rnSt5Am = item.rnSt5Am
    rnSt5Pm = item.rnSt5Pm
    rnSt6Am = item.rnSt6Am
    rnSt6Pm = item.rnSt6Pm
    rnSt7Am = item.rnSt7Am
    rnSt7Pm = item.rnSt7Pm
    rnSt8 = item.rnSt8
    rnSt9 = item.rnSt9
    rnSt10 = item.rnSt10
}

fun WeatherMidForecast.applyTa(item: MidTaItem) {
    taMin3 = item.taMin3
    taMax3 = item.taMax3
    taMin4 = item.taMin4
    taMax4 = item.taMax4
    taMin5 = item.taMin5
    taMax5 = item.taMax5
    taMin6 = item.taMin6
    taMax6 = item.taMax6
    taMin7 = item.taMin7
    taMax7 = item.taMax7
    taMin8 = item.taMin8
    taMax8 = item.taMax8
    taMin9 = item.taMin9
    taMax9 = item.taMax9
    taMin10 = item.taMin10
    taMax10 = item.taMax10
}

fun VilageFcstItem.toShortForecast(): WeatherShortForecast = WeatherShortForecast(
    nx = nx,
    ny = ny,
    baseDate = baseDate,
    baseTime = baseTime,
    fcstDate = fcstDate,
    fcstTime = fcstTime,
    category = category,
    fcstValue = fcstValue,
)

fun WeatherShortForecast.applyUpdate(item: VilageFcstItem) {
    baseDate = item.baseDate
    baseTime = item.baseTime
    fcstValue = item.fcstValue
}
