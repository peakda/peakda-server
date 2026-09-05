package com.peakda.server.infrastructure.external.kma.asosdaly

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
class AsosStationCatalog(
    @Value("\${external.kma.asos-daly.station-catalog:classpath:external/kma/asos-stations.csv}")
    resource: Resource,
) {
    val all: List<AsosStation> = parse(resource)

    private fun parse(resource: Resource): List<AsosStation> {
        val stations = resource.inputStream.bufferedReader().useLines { lines ->
            lines.map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith(COMMENT_PREFIX) && !it.startsWith(HEADER_PREFIX) }
                .map { toStation(it, resource) }
                .toList()
        }
        require(stations.isNotEmpty()) { "ASOS 지점 카탈로그가 비어 있습니다. resource=$resource" }
        return stations
    }

    private fun toStation(line: String, resource: Resource): AsosStation {
        val columns = line.split(COLUMN_DELIMITER)
        require(columns.size >= MIN_COLUMNS) {
            "ASOS 지점 카탈로그 형식이 올바르지 않습니다. resource=$resource line=$line"
        }
        return try {
            AsosStation(
                stnId = columns[0].trim(),
                name = columns[1].trim(),
                latitude = columns[2].trim().toDouble(),
                longitude = columns[3].trim().toDouble(),
                altitude = columns[4].trim().takeIf { it.isNotEmpty() }?.toDouble(),
            )
        } catch (exception: NumberFormatException) {
            throw IllegalArgumentException(
                "ASOS 지점 카탈로그 숫자 형식이 올바르지 않습니다. resource=$resource line=$line",
                exception,
            )
        }
    }

    companion object {
        private const val COMMENT_PREFIX = "#"
        private const val HEADER_PREFIX = "stnId,"
        private const val COLUMN_DELIMITER = ","

        /** stnId, name, latitude, longitude, altitude */
        private const val MIN_COLUMNS = 5
    }
}

data class AsosStation(
    val stnId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    /** 노장 해발고도(m). 명소 고도 원천이 없어 현재 판정에는 쓰지 않는다. */
    val altitude: Double?,
)
