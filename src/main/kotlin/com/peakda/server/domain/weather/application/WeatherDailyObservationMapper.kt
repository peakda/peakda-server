package com.peakda.server.domain.weather.application

import com.peakda.server.domain.weather.repository.WeatherDailyObservationUpsertCommand
import com.peakda.server.infrastructure.external.kma.asosdaly.response.AsosDalyItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

fun AsosDalyItem.toUpsertCommand(): WeatherDailyObservationUpsertCommand? {
    val stationId = stnId.trim().takeIf { it.isNotEmpty() } ?: return null
    val observedOn = tm.trim().toObservationDateOrNull() ?: return null

    return WeatherDailyObservationUpsertCommand(
        stationId = stationId,
        observedOn = observedOn,
        stationName = stnNm.trim().ifBlank { null },
        avgTemperature = avgTa.trim().toDoubleOrNull(),
        minTemperature = minTa.trim().toDoubleOrNull(),
        maxTemperature = maxTa.trim().toDoubleOrNull(),
    )
}

private fun String.toObservationDateOrNull(): LocalDate? {
    return try {
        LocalDate.parse(this, DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (_: DateTimeParseException) {
        try {
            LocalDate.parse(this, DateTimeFormatter.BASIC_ISO_DATE)
        } catch (_: DateTimeParseException) {
            null
        }
    }
}
