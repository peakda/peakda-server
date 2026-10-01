package com.peakda.server.infrastructure.external.kma.flower

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

/** 기상청 유명산 단풍 관측 산 → 산이 걸친 법정동 시도 코드. */
@Component
class MapleObservationMountainCatalog(
    @Value("\${external.kma.flower-observation.maple-mountains:classpath:external/kma/maple-observation-mountains.csv}")
    resource: Resource,
) {
    /** 관측 장소(산 이름) → 법정동 시도 코드. */
    val areaCodesByMountain: Map<String, Set<String>> = parse(resource)

    private fun parse(resource: Resource): Map<String, Set<String>> {
        val mountains = resource.inputStream.bufferedReader().useLines { lines ->
            lines.map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith(COMMENT_PREFIX) && !it.startsWith(HEADER_PREFIX) }
                .map { toEntry(it, resource) }
                .toMap()
        }
        require(mountains.isNotEmpty()) { "기상청 단풍 관측 산 목록이 비어 있습니다. resource=$resource" }
        return mountains
    }

    private fun toEntry(line: String, resource: Resource): Pair<String, Set<String>> {
        val columns = line.split(COLUMN_DELIMITER)
        val areaCodes = columns.getOrNull(1).orEmpty().split(CODE_DELIMITER).map { it.trim() }.filter { it.isNotEmpty() }
        require(columns.size == COLUMNS && columns[0].isNotBlank() && areaCodes.isNotEmpty()) {
            "기상청 단풍 관측 산 형식이 올바르지 않습니다. resource=$resource line=$line"
        }
        return columns[0].trim() to areaCodes.toSet()
    }

    companion object {
        private const val COMMENT_PREFIX = "#"
        private const val HEADER_PREFIX = "obsPlace,"
        private const val COLUMN_DELIMITER = ","
        private const val CODE_DELIMITER = ";"

        /** obsPlace, areaCodes */
        private const val COLUMNS = 2
    }
}
