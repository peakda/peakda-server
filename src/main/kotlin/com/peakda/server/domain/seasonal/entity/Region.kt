package com.peakda.server.domain.seasonal.entity

/**
 * 지도 권역 필터. areaCode 는 Tour API 의 지역 코드와 동일한 문자열이다.
 */
enum class Region(
    val displayName: String,
    val subtitle: String,
    val areaCodes: Set<String>,
) {
    CAPITAL("수도권", "서울 · 경기 · 인천 등", setOf("1", "2", "31")),
    GANGWON("강원", "강릉 · 속초 · 춘천 등", setOf("32")),
    CHUNGCHEONG("충청", "대전 · 공주 · 천안 등", setOf("3", "8", "33", "34")),
    GYEONGSANG("경상", "부산 · 경주 · 진해 등", setOf("4", "6", "7", "35", "36")),
    JEOLLA("전라", "광주 · 전주 · 순천 등", setOf("5", "37", "38")),
    JEJU("제주", "제주 · 서귀포", setOf("39")),
    ;

    companion object {
        private val BY_AREA_CODE: Map<String, Region> = entries
            .flatMap { region -> region.areaCodes.map { areaCode -> areaCode to region } }
            .toMap()

        private val ADDRESS_PREFIXES: Map<Region, Set<String>> = mapOf(
            CAPITAL to setOf("서울", "인천", "경기"),
            GANGWON to setOf("강원"),
            CHUNGCHEONG to setOf("대전", "세종", "충북", "충청북도", "충남", "충청남도"),
            GYEONGSANG to setOf("대구", "부산", "울산", "경북", "경상북도", "경남", "경상남도"),
            JEOLLA to setOf("광주", "전북", "전라북도", "전남", "전라남도"),
            JEJU to setOf("제주"),
        )

        fun ofAreaCode(areaCode: String): Region? = BY_AREA_CODE[areaCode.trim()]

        /** 주소의 첫 토큰(시도)을 이용한 LOCAL Spot 용 폴백 판정. */
        fun ofAddress(address: String?): Region? {
            val firstToken = address?.trim()?.split(Regex("\\s+"))?.firstOrNull() ?: return null
            return ADDRESS_PREFIXES.entries.firstOrNull { (_, prefixes) ->
                prefixes.any { prefix -> firstToken.startsWith(prefix) }
            }?.key
        }
    }
}
