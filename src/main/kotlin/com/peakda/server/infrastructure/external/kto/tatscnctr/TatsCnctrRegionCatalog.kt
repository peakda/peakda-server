package com.peakda.server.infrastructure.external.kto.tatscnctr

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

/**
 * 집중률 조회 대상 시군구 목록.
 *
 * `tatsCnctrRatedList` 는 areaCd·signguCd 가 필수라 전국을 한 번에 받을 수 없고,
 * 시군구를 순회하는 것 말고는 방법이 없다. 목록은 한국관광공사가 활용매뉴얼과 함께
 * 배포하는 코드파일에서 온 참조 자료이므로 classpath 리소스로 관리한다.
 * 행정구역 개편 시 CSV 만 교체하면 되고 배포/마이그레이션 절차가 필요 없다.
 */
@Component
class TatsCnctrRegionCatalog(
    @Value("\${external.kto.tats-cnctr.region-codes:classpath:external/kto/tats-cnctr-sigungu-codes.csv}")
    resource: Resource,
) {
    val all: List<TatsCnctrRegion> = parse(resource)

    private fun parse(resource: Resource): List<TatsCnctrRegion> {
        val regions = resource.inputStream.bufferedReader().useLines { lines ->
            lines.map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith(COMMENT_PREFIX) && !it.startsWith(HEADER_PREFIX) }
                .map { toRegion(it, resource) }
                .toList()
        }
        require(regions.isNotEmpty()) { "집중률 시군구 코드 파일이 비어 있습니다. resource=$resource" }
        return regions
    }

    private fun toRegion(line: String, resource: Resource): TatsCnctrRegion {
        val columns = line.split(COLUMN_DELIMITER)
        require(columns.size >= MIN_COLUMNS) {
            "집중률 시군구 코드 형식이 올바르지 않습니다. resource=$resource line=$line"
        }
        return TatsCnctrRegion(areaCd = columns[0].trim(), signguCd = columns[2].trim())
    }

    companion object {
        private const val COMMENT_PREFIX = "#"
        private const val HEADER_PREFIX = "areaCd,"
        private const val COLUMN_DELIMITER = ","

        /** areaCd, areaNm, sigunguCd, sigunguNm — 이름 컬럼은 사람이 읽기 위한 것이다. */
        private const val MIN_COLUMNS = 4
    }
}
