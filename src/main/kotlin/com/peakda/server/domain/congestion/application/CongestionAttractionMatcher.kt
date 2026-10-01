package com.peakda.server.domain.congestion.application

import com.peakda.server.domain.attraction.repository.AttractionNameCandidate
import com.peakda.server.domain.congestion.entity.CongestionLinkStatus
import com.peakda.server.domain.congestion.entity.CongestionMatchType

/**
 * 집중률 관광지명 → 명소 매칭 규칙.
 *
 * 2026-09 운영 데이터 측정(집중률 관광지 3,126개)에서 같은 시군구 유일 정확 일치가 78%,
 * 개편 코드 보정 정확 일치가 추가 5%였다. 포함 관계 일치(3%)는 동명 시설이 섞여 자동 확정하지 않는다.
 */
object CongestionAttractionMatcher {
    /**
     * 행정구역 개편으로 집중률(개편 전 코드)과 명소(개편 후 법정동 코드)의 시도 코드가 달라진 경우.
     * 광주(29)·전남(46) → 전남광주통합특별시(12). 인천(28)·세종(36)은 시도 코드는 같고 시군구만 바뀌었다.
     */
    private val REFORMED_AREA_CODES: Map<String, Set<String>> = mapOf(
        "29" to setOf("12"),
        "46" to setOf("12"),
    )

    private const val MAX_CANDIDATES = 5
    private const val MIN_CONTAINS_LENGTH = 2

    fun reformAreaCodes(areaCode: String): Set<String> = REFORMED_AREA_CODES[areaCode] ?: setOf(areaCode)

    /**
     * @param sameSigungu 같은 법정동 시군구의 명소 후보
     * @param reformArea 같은 시군구 후보가 하나도 없을 때(=시군구 코드가 개편으로 사라짐)만 조회하는 개편 후 시도 범위 후보.
     *   호출측은 [sameSigungu] 가 비어 있을 때만 이 목록을 채운다.
     */
    fun decide(
        name: String,
        sameSigungu: List<AttractionNameCandidate>,
        reformArea: List<AttractionNameCandidate> = emptyList(),
    ): CongestionLinkDecision {
        val target = normalize(name)
        if (target.isEmpty()) return unmatched()

        val exact = sameSigungu.filter { normalize(it.title) == target }
        if (exact.size == 1) return confirmed(CongestionMatchType.EXACT, exact.single().id)
        if (exact.size > 1) return pending(CongestionMatchType.EXACT, exact)

        if (target.length >= MIN_CONTAINS_LENGTH) {
            val contains = sameSigungu.filter { candidate ->
                val title = normalize(candidate.title)
                title.length >= MIN_CONTAINS_LENGTH && (title.contains(target) || target.contains(title))
            }
            if (contains.isNotEmpty()) return pending(CongestionMatchType.CONTAINS, contains)
        }

        if (sameSigungu.isEmpty()) {
            val reformExact = reformArea.filter { normalize(it.title) == target }
            if (reformExact.size == 1) return confirmed(CongestionMatchType.REGION_REFORM, reformExact.single().id)
            if (reformExact.size > 1) return pending(CongestionMatchType.REGION_REFORM, reformExact)
        }
        return unmatched()
    }

    /** 괄호 안 부연 설명과 공백·특수문자를 제거하고 소문자로 맞춘다. 예: `무민사(남해)` → `무민사`. */
    fun normalize(name: String): String =
        name.replace(BRACKETED, "").filter { it.isLetterOrDigit() }.lowercase()

    private val BRACKETED = Regex("""\(.*?\)|\[.*?]""")

    private fun confirmed(type: CongestionMatchType, attractionId: Long) =
        CongestionLinkDecision(CongestionLinkStatus.CONFIRMED, type, attractionId, emptyList())

    /** 후보가 하나면 그 명소를 미리 채워 관리자가 확정만 누르면 되게 한다. */
    private fun pending(type: CongestionMatchType, candidates: List<AttractionNameCandidate>) =
        CongestionLinkDecision(
            status = CongestionLinkStatus.PENDING_REVIEW,
            matchType = type,
            attractionId = candidates.singleOrNull()?.id,
            candidateIds = candidates.take(MAX_CANDIDATES).map { it.id },
        )

    private fun unmatched() = CongestionLinkDecision(CongestionLinkStatus.UNMATCHED, null, null, emptyList())
}
