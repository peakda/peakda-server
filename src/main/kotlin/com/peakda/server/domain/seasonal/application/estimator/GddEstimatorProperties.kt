package com.peakda.server.domain.seasonal.application.estimator

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * GDD 추정기(신호 A) 튜닝값 — **MVP 스캐폴딩**.
 *
 * 과거 실측 일평균기온(KMA ASOS) 수집 파이프라인이 추가되기 전까지 [enabled] 는 false 이며 추정기는 항상 null 을 반환한다.
 * [thresholds] 는 카테고리명(`BloomCategory.name`) → 종별 기준온도·누적 임계치 매핑이다.
 */
@ConfigurationProperties(prefix = "peakda.timing.gdd")
data class GddEstimatorProperties(
    val enabled: Boolean = false,
    val thresholds: Map<String, GddThreshold> = emptyMap(),
) {
    data class GddThreshold(
        /** 생장 기준온도(°C). 일평균기온에서 이 값을 뺀 만큼만 누적한다. */
        val tBase: Double = 5.4,
        /** 개화 시작(STARTED) 누적 GDD 임계치. */
        val start: Double = 90.0,
        /** 절정(PEAK) 누적 GDD 임계치. */
        val peak: Double = 110.0,
        /** 종료(ENDED) 누적 GDD 임계치. */
        val end: Double = 130.0,
    )
}
