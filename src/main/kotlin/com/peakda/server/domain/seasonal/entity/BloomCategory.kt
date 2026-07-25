package com.peakda.server.domain.seasonal.entity

import com.peakda.server.domain.spot.entity.Season
import java.time.MonthDay

/**
 * 타이밍 산출의 단위가 되는 꽃·계절 카테고리 (MVP 13종).
 *
 * 타이밍 도메인의 **정본**이다. 사용자 제안형 `plants` 테이블과는 `plants.bloom_category` 브릿지 컬럼으로 연결된다.
 *
 * - [season] 은 기존 `spot.Season`(SPRING/SUMMER/AUTUMN_WINTER) 을 재사용한다 (동백은 겨울이지만 AUTUMN_WINTER 로 그룹).
 * - [typicalPeakRange] 는 평년 절정 기간의 대략값으로 Calendar 추정기의 prior 이며 운영 튜닝 대상이다.
 * - [keywordHints] / [festivalHints] 는 자동 태깅(키워드·축제) 매칭 토큰이다.
 * - GDD 임계치·기준온도 등 물리 상수는 enum 이 아닌 application.yml(`peakda.timing.gdd`)에서 관리한다.
 */
enum class BloomCategory(
    val displayName: String,
    val season: Season,
    val typicalPeakRange: MonthDayRange,
    val keywordHints: List<String>,
    val festivalHints: List<String>,
) {
    PLUM(
        displayName = "매화",
        season = Season.SPRING,
        typicalPeakRange = MonthDayRange(MonthDay.of(3, 5), MonthDay.of(3, 25)),
        keywordHints = listOf("매화", "매실", "plum"),
        festivalHints = listOf("매화"),
    ),
    FORSYTHIA(
        displayName = "개나리",
        season = Season.SPRING,
        typicalPeakRange = MonthDayRange(MonthDay.of(3, 25), MonthDay.of(4, 10)),
        keywordHints = listOf("개나리", "forsythia"),
        festivalHints = listOf("개나리"),
    ),
    AZALEA_KR(
        displayName = "진달래",
        season = Season.SPRING,
        typicalPeakRange = MonthDayRange(MonthDay.of(3, 28), MonthDay.of(4, 15)),
        keywordHints = listOf("진달래", "azalea"),
        festivalHints = listOf("진달래"),
    ),
    CHERRY(
        displayName = "벚꽃",
        season = Season.SPRING,
        typicalPeakRange = MonthDayRange(MonthDay.of(3, 25), MonthDay.of(4, 15)),
        keywordHints = listOf("벚꽃", "벚나무", "cherry blossom", "사쿠라"),
        festivalHints = listOf("벚꽃", "군항제"),
    ),
    CANOLA(
        displayName = "유채",
        season = Season.SPRING,
        typicalPeakRange = MonthDayRange(MonthDay.of(3, 20), MonthDay.of(4, 30)),
        keywordHints = listOf("유채", "청보리", "canola"),
        festivalHints = listOf("유채", "청보리"),
    ),
    AZALEA(
        displayName = "철쭉",
        season = Season.SPRING,
        typicalPeakRange = MonthDayRange(MonthDay.of(4, 20), MonthDay.of(5, 15)),
        keywordHints = listOf("철쭉", "royal azalea"),
        festivalHints = listOf("철쭉"),
    ),
    HYDRANGEA(
        displayName = "수국",
        season = Season.SUMMER,
        typicalPeakRange = MonthDayRange(MonthDay.of(6, 10), MonthDay.of(7, 15)),
        keywordHints = listOf("수국", "hydrangea"),
        festivalHints = listOf("수국"),
    ),
    LOTUS(
        displayName = "연꽃",
        season = Season.SUMMER,
        typicalPeakRange = MonthDayRange(MonthDay.of(7, 1), MonthDay.of(8, 15)),
        keywordHints = listOf("연꽃", "연밭", "lotus"),
        festivalHints = listOf("연꽃"),
    ),
    COSMOS(
        displayName = "코스모스",
        season = Season.AUTUMN_WINTER,
        typicalPeakRange = MonthDayRange(MonthDay.of(9, 10), MonthDay.of(10, 15)),
        keywordHints = listOf("코스모스", "cosmos"),
        festivalHints = listOf("코스모스"),
    ),
    PINK_MUHLY(
        displayName = "핑크뮬리",
        season = Season.AUTUMN_WINTER,
        typicalPeakRange = MonthDayRange(MonthDay.of(9, 20), MonthDay.of(10, 25)),
        keywordHints = listOf("핑크뮬리", "뮬리", "pink muhly"),
        festivalHints = listOf("핑크뮬리"),
    ),
    SILVERGRASS(
        displayName = "억새",
        season = Season.AUTUMN_WINTER,
        typicalPeakRange = MonthDayRange(MonthDay.of(10, 1), MonthDay.of(11, 10)),
        keywordHints = listOf("억새", "갈대", "silvergrass", "pampas"),
        festivalHints = listOf("억새", "갈대"),
    ),
    MAPLE(
        displayName = "단풍",
        season = Season.AUTUMN_WINTER,
        typicalPeakRange = MonthDayRange(MonthDay.of(10, 20), MonthDay.of(11, 15)),
        keywordHints = listOf("단풍", "단풍나무", "maple", "autumn leaves"),
        festivalHints = listOf("단풍"),
    ),
    CAMELLIA(
        displayName = "동백",
        season = Season.AUTUMN_WINTER,
        typicalPeakRange = MonthDayRange(MonthDay.of(12, 1), MonthDay.of(3, 15)),
        keywordHints = listOf("동백", "camellia"),
        festivalHints = listOf("동백"),
    ),
    ;

    /**
     * GDD(누적기온) 모델이 적합한 온도 의존종 여부. 비온도 의존종(수국·연꽃·코스모스·핑크뮬리·억새·동백)은 false.
     */
    val temperatureDriven: Boolean
        get() = this in TEMPERATURE_DRIVEN

    companion object {
        private val TEMPERATURE_DRIVEN =
            setOf(CHERRY, PLUM, FORSYTHIA, AZALEA_KR, AZALEA, CANOLA, MAPLE)

        /**
         * 축제명에 [festivalHints]가 포함되는 첫 카테고리.
         * 자동 태깅과 탐색 큐레이션이 공유하는 꽃축제 판정 규칙이다.
         */
        fun ofFestivalName(festivalName: String): BloomCategory? {
            val haystack = festivalName.lowercase()
            return entries.firstOrNull { category -> category.festivalHints.any { haystack.contains(it.lowercase()) } }
        }
    }
}
