package com.peakda.server.infrastructure.external.kma.flower

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

/** 기상청 유명산 단풍 관측 산 → 산이 걸친 법정동 시도 코드와 제외어. */
@Component
class MapleObservationMountainCatalog(
    @Value("\${external.kma.flower-observation.maple-mountains:classpath:external/kma/maple-observation-mountains.csv}")
    resource: Resource,
) {
    val mountains: List<MapleObservationMountain> = parse(resource)

    private fun parse(resource: Resource): List<MapleObservationMountain> {
        val mountains = resource.inputStream.bufferedReader().useLines { lines ->
            lines.map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith(COMMENT_PREFIX) && !it.startsWith(HEADER_PREFIX) }
                .map { toEntry(it, resource) }
                .toList()
        }
        require(mountains.isNotEmpty()) { "기상청 단풍 관측 산 목록이 비어 있습니다. resource=$resource" }
        return mountains
    }

    private fun toEntry(line: String, resource: Resource): MapleObservationMountain {
        val columns = line.split(COLUMN_DELIMITER)
        val areaCodes = columns.getOrNull(1).splitValues()
        require(columns.size in MIN_COLUMNS..MAX_COLUMNS && columns[0].isNotBlank() && areaCodes.isNotEmpty()) {
            "기상청 단풍 관측 산 형식이 올바르지 않습니다. resource=$resource line=$line"
        }
        return MapleObservationMountain(
            name = columns[0].trim(),
            areaCodes = areaCodes.toSet(),
            excludeKeywords = columns.getOrNull(2).splitValues(),
        )
    }

    private fun String?.splitValues(): List<String> =
        orEmpty().split(VALUE_DELIMITER).map { it.trim() }.filter { it.isNotEmpty() }

    companion object {
        private const val COMMENT_PREFIX = "#"
        private const val HEADER_PREFIX = "obsPlace,"
        private const val COLUMN_DELIMITER = ","
        private const val VALUE_DELIMITER = ";"

        /** obsPlace, areaCodes[, excludeKeywords] */
        private const val MIN_COLUMNS = 2
        private const val MAX_COLUMNS = 3
    }
}

/** 관측 산 한 줄. [excludeKeywords] 는 시도 코드로 못 거르는 동명이산 등의 제목 단어다. */
data class MapleObservationMountain(
    val name: String,
    val areaCodes: Set<String>,
    val excludeKeywords: List<String>,
)
