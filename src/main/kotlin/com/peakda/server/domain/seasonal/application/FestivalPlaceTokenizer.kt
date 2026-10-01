package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.seasonal.entity.BloomCategory

/**
 * 축제 장소명(`venue`)·축제명에서 명소 제목과 대조할 장소 토큰을 뽑는다.
 *
 * 공공데이터 축제 좌표는 주최 기관 주소로 들어오는 경우가 있어, 축제 ↔ 명소 연결은 좌표가 아니라 장소명 일치로 판정한다.
 * - venue: 괄호·구분자로 나누고 `일원`·`시설지구` 같은 일반 접미사를 뗀다. 예: `팔공산 갓바위 시설지구 일원` → {팔공산, 갓바위}
 * - 축제명: 회차·연도·꽃 이름·`축제` 조각을 버린다. 지역명(`포천`, `하동`)은 주소에 들어 있으면 버린다.
 *   venue 보다 잡음이 많아 3글자 이상만 쓰고, `춤추는` 같은 관형형 조각은 버린다.
 *   예: `2026년 제25회 팔공산 단풍축제` → {팔공산}
 * - 두 출처 모두 주소의 시·군·구 이름과 시·도 약칭(`광양`, `강진`, `전남`)과 같은 토큰은 버린다.
 *   지역명 토큰은 그 지역 명소 제목 대부분에 들어 있어 축제와 무관한 명소까지 끌어온다.
 *   예: `광양 매화마을 일원` + 주소 `전라남도 광양시 …` → {매화마을}
 * 토큰은 공백 제거·소문자로 정규화되며, [normalize] 한 명소 제목과 비교한다.
 */
object FestivalPlaceTokenizer {

    private val SEPARATORS = Regex("""[\s()\[\]{}<>「」『』~,，·・/&+\-:'"“”‘’]+""")
    private val GENERIC_SUFFIXES = listOf("시설지구", "특설무대", "행사장", "일원", "일대", "주변", "광장")
    private val STOPWORDS = setOf(
        "공원", "생태공원", "체육공원", "시민공원", "호수공원", "한강공원", "근린공원", "수변공원",
        "국립공원", "도립공원", "군립공원",
        "종합운동장", "운동장", "체육관", "문화회관", "주차장", "시내", "전역", "관내", "야외", "특설",
        "마라톤", "걷기대회", "대회", "행사", "체험", "야행", "음악회", "콘서트",
    )
    private val ADNOMINAL = Regex("""^.+[는은]$""")
    private val ADMIN_UNIT = Regex("""^.{1,3}(시|군|구|읍|면|동|리)$""")
    private val ORDINAL = Regex("""^(제?\d.*)$""")
    private val FESTIVAL_WORDS = listOf("축제", "페스티벌", "한마당", "문화제", "큰잔치")
    private val FLOWER_WORDS: List<String> = BloomCategory.entries
        .flatMap { it.festivalHints + it.keywordHints + it.displayName }
        .distinct()
    private val REGION_UNIT = Regex("""^(.{2,}?)(특별자치시|특별자치도|통합특별시|특별시|광역시|시|군|구|도)$""")
    private val PROVINCE_SHORT_NAMES = setOf(
        "서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종",
        "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주",
    )

    fun tokenize(venue: String, festivalName: String, addresses: List<String?>): Set<String> {
        val regionNames = PROVINCE_SHORT_NAMES + regionNamesOf(addresses)
        val normalizedAddress = addresses.filterNotNull().joinToString("") { normalize(it) }
        val venueTokens = venue.split(SEPARATORS).map(::stripGenericSuffix)
        val nameTokens = festivalName.split(SEPARATORS)
            .filterNot { ORDINAL.matches(it) }
            .filterNot { piece -> FESTIVAL_WORDS.any { piece.contains(it) } }
            .filterNot { piece -> FLOWER_WORDS.any { piece.contains(it) } }
            .filterNot { normalizedAddress.contains(normalize(it)) }
            .filterNot { ADNOMINAL.matches(it) }
            .filter { normalize(it).length >= MIN_NAME_TOKEN_LENGTH }
        return (venueTokens + nameTokens)
            .map(::normalize)
            .filter(::isPlaceToken)
            .filterNot { it in regionNames }
            .toSet()
    }

    fun normalize(text: String): String = text.replace(Regex("""\s+"""), "").lowercase()

    /** 주소 앞쪽(시·도, 시·군·구) 조각에서 행정 단위 접미사를 뗀 지역명. 예: `광양시` → `광양`, `강진군` → `강진`. */
    private fun regionNamesOf(addresses: List<String?>): Set<String> =
        addresses.filterNotNull()
            .flatMap { it.trim().split(Regex("""\s+""")).take(ADDRESS_REGION_PIECES) }
            .mapNotNull { piece -> REGION_UNIT.matchEntire(piece)?.groupValues?.get(1) }
            .map(::normalize)
            .toSet()

    private fun stripGenericSuffix(piece: String): String {
        var result = piece
        while (true) {
            val suffix = GENERIC_SUFFIXES.firstOrNull { result.endsWith(it) } ?: return result
            result = result.removeSuffix(suffix)
        }
    }

    private fun isPlaceToken(token: String): Boolean =
        token.length >= MIN_TOKEN_LENGTH && token !in STOPWORDS && !ADMIN_UNIT.matches(token)

    private const val MIN_TOKEN_LENGTH = 2
    private const val MIN_NAME_TOKEN_LENGTH = 3
    private const val ADDRESS_REGION_PIECES = 3
}
