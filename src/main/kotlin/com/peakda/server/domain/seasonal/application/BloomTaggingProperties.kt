package com.peakda.server.domain.seasonal.application

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
)
