package com.peakda.server.domain.explore.application

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "peakda.explore")
data class ExploreProperties(
    /** "지금이 절정이에요" 가로 스크롤 카드 수. */
    val peakNowSize: Int = 10,

    /** "다음 주에 가면 좋을 곳" 리스트 카드 수 (Figma 5개). */
    val nextWeekSize: Int = 5,

    /** "지금 열리는 축제" 카드 수. */
    val festivalSize: Int = 10,

    /** 진행 중 축제 후보 조회 상한. 꽃축제만 남기므로 여유를 둔다. */
    val festivalCandidateSize: Int = 200,

    /** 탐색 화면에 노출할 큐레이션 카드 수. */
    val curationSize: Int = 5,
)
