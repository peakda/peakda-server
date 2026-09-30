package com.peakda.server.domain.attraction.application

/**
 * TourAPI 법정동 코드(`lDongRegnCd`, `lDongSignguCd`) 정규화.
 *
 * 대부분 시도 2자리 + 시군구 3자리로 오지만, 세종처럼 두 필드에 5자리 코드가 그대로 겹쳐 오는 경우가
 * 있다(`36110` / `36110`). 관광지 집중률 API 의 `signguCd` 와 같은 5자리 형태로 맞춘다.
 */
object LegalDongCode {
    fun areaCode(regionCode: String): String? =
        regionCode.trim().take(AREA_LENGTH).takeIf { it.length == AREA_LENGTH && it.all(Char::isDigit) }

    fun sigunguCode(regionCode: String, sigunguCode: String): String? {
        val region = regionCode.trim()
        val sigungu = sigunguCode.trim()
        val code = when {
            sigungu.length == SIGUNGU_LENGTH -> sigungu
            region.length == AREA_LENGTH && sigungu.length == SIGUNGU_SUFFIX_LENGTH -> region + sigungu
            region.length == SIGUNGU_LENGTH -> region
            else -> return null
        }
        return code.takeIf { it.all(Char::isDigit) }
    }

    private const val AREA_LENGTH = 2
    private const val SIGUNGU_SUFFIX_LENGTH = 3
    private const val SIGUNGU_LENGTH = 5
}
