package com.peakda.server.infrastructure.external.kma.flower

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
class FlowerObservationStationCatalog(
    @Value("\${external.kma.flower-observation.station-map:classpath:external/kma/flower-observation-stations.csv}")
    resource: Resource,
) {
    /** 관측 장소명 → 지점번호. */
    val stationByPlace: Map<String, String> = parse(resource)

    private fun parse(resource: Resource): Map<String, String> {
        val stations = resource.inputStream.bufferedReader().useLines { lines ->
            lines.map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith(COMMENT_PREFIX) && !it.startsWith(HEADER_PREFIX) }
                .map { toStationEntry(it, resource) }
                .toMap()
        }
        require(stations.isNotEmpty()) { "기상청 꽃 관측지점 매핑이 비어 있습니다. resource=$resource" }
        return stations
    }

    private fun toStationEntry(line: String, resource: Resource): Pair<String, String> {
        val columns = line.split(COLUMN_DELIMITER)
        require(columns.size >= MIN_COLUMNS) {
            "기상청 꽃 관측지점 매핑 형식이 올바르지 않습니다. resource=$resource line=$line"
        }
        return columns[0].trim() to columns[1].trim()
    }

    companion object {
        private const val COMMENT_PREFIX = "#"
        private const val HEADER_PREFIX = "obsPlace,"
        private const val COLUMN_DELIMITER = ","

        /** obsPlace, stnId, stnName */
        private const val MIN_COLUMNS = 3
    }
}
