package com.peakda.server.domain.congestion.application

import com.peakda.server.domain.attraction.repository.AttractionNameCandidate
import com.peakda.server.domain.congestion.entity.CongestionLinkStatus
import com.peakda.server.domain.congestion.entity.CongestionMatchType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CongestionAttractionMatcherTest {
    private fun candidate(id: Long, title: String) = AttractionNameCandidate(id, title)

    @Test
    fun `같은 시군구에 정규화한 이름이 하나만 일치하면 자동 확정한다`() {
        val decision = CongestionAttractionMatcher.decide(
            "석촌 호수",
            listOf(candidate(1, "석촌호수"), candidate(2, "롯데월드")),
        )

        assertThat(decision.status).isEqualTo(CongestionLinkStatus.CONFIRMED)
        assertThat(decision.matchType).isEqualTo(CongestionMatchType.EXACT)
        assertThat(decision.attractionId).isEqualTo(1)
    }

    @Test
    fun `괄호 안 부연 설명은 무시하고 비교한다`() {
        val decision = CongestionAttractionMatcher.decide("용마랜드", listOf(candidate(7, "용마랜드(용마공원놀이동산)")))

        assertThat(decision.status).isEqualTo(CongestionLinkStatus.CONFIRMED)
        assertThat(decision.attractionId).isEqualTo(7)
    }

    @Test
    fun `정확 일치가 여럿이면 후보만 남기고 검토 대기로 둔다`() {
        val decision = CongestionAttractionMatcher.decide(
            "용강서원",
            listOf(candidate(1, "용강서원"), candidate(2, "용강 서원")),
        )

        assertThat(decision.status).isEqualTo(CongestionLinkStatus.PENDING_REVIEW)
        assertThat(decision.attractionId).isNull()
        assertThat(decision.candidateIds).containsExactly(1L, 2L)
    }

    @Test
    fun `이름이 포함 관계로만 맞으면 자동 확정하지 않고 유일 후보를 제안한다`() {
        val decision = CongestionAttractionMatcher.decide("수도사", listOf(candidate(3, "석암산 수도사")))

        assertThat(decision.status).isEqualTo(CongestionLinkStatus.PENDING_REVIEW)
        assertThat(decision.matchType).isEqualTo(CongestionMatchType.CONTAINS)
        assertThat(decision.attractionId).isEqualTo(3)
    }

    @Test
    fun `같은 시군구 후보가 없으면 개편 후 시도 범위의 유일 정확 일치로 확정한다`() {
        val decision = CongestionAttractionMatcher.decide(
            "인천 팔미도 등대",
            sameSigungu = emptyList(),
            reformArea = listOf(candidate(9, "인천 팔미도 등대"), candidate(10, "월미도")),
        )

        assertThat(decision.status).isEqualTo(CongestionLinkStatus.CONFIRMED)
        assertThat(decision.matchType).isEqualTo(CongestionMatchType.REGION_REFORM)
        assertThat(decision.attractionId).isEqualTo(9)
    }

    @Test
    fun `같은 시군구에 명소가 있으면 다른 시군구의 동명 명소로 넘어가지 않는다`() {
        val decision = CongestionAttractionMatcher.decide(
            "무민사",
            sameSigungu = listOf(candidate(1, "해운대해수욕장")),
            reformArea = listOf(candidate(2, "무민사")),
        )

        assertThat(decision.status).isEqualTo(CongestionLinkStatus.UNMATCHED)
        assertThat(decision.attractionId).isNull()
    }

    @Test
    fun `광주와 전남의 개편 전 시도 코드는 전남광주통합특별시 코드로 바꿔 찾는다`() {
        assertThat(CongestionAttractionMatcher.reformAreaCodes("29")).containsExactly("12")
        assertThat(CongestionAttractionMatcher.reformAreaCodes("46")).containsExactly("12")
        assertThat(CongestionAttractionMatcher.reformAreaCodes("28")).containsExactly("28")
    }
}
