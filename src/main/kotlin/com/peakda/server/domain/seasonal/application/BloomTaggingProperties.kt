package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.seasonal.entity.BloomCategory
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 자동 태깅 신뢰도·근접 임계치. 운영 중 yml 로 튜닝한다.
 */
@ConfigurationProperties(prefix = "peakda.timing.tagging")
data class BloomTaggingProperties(
    /** 신호 A 키워드 매칭 기본 신뢰도. */
    val keywordBaseConfidence: Double = 0.5,
    /** 제목에 카테고리명이 정확히 포함될 때 가산. */
    val keywordExactBoost: Double = 0.2,
    /** 신호 B 축제 장소명 매칭 신뢰도. */
    val festivalConfidence: Double = 0.9,
    /** 축제 장소명 매칭 후보를 찾는 반경(km). 축제 좌표가 주최 기관 주소일 수 있어 넉넉히 잡는다. */
    val festivalCandidateRadiusKm: Double = 20.0,
    /** 축제 종료 후 FESTIVAL 태그를 유지하는 기간(일). 내년 축제 데이터가 들어오기 전까지 명소가 꽃 목록에서 빠지지 않게 한다. */
    val festivalTagRetentionDays: Long = 365,
    /** 신호 C TourAPI 소분류 매칭 신뢰도. 유형만 보고 붙이므로 키워드보다 낮게 둔다. */
    val categoryConfidence: Double = 0.4,
    /** 신호 C 카테고리별 TourAPI 소분류(cat3) 코드. 예: 단풍 ← 국립공원 `A01010100`. */
    val categoryTags: Map<BloomCategory, Set<String>> = emptyMap(),
    /** 신호 D 기상청 유명산 단풍 관측 산 매칭 신뢰도. 직접 관측 대상이라 가장 높게 둔다. */
    val observationConfidence: Double = 0.9,
)
