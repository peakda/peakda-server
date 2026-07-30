package com.peakda.server.domain.seasonal.application.estimator

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * GDD 추정기(신호 A) 튜닝값.
 *
 * [thresholds] 는 카테고리명(`BloomCategory.name`) → 종별 기준온도·누적 임계치 매핑이다.
 */
@ConfigurationProperties(prefix = "peakda.timing.gdd")
data class GddEstimatorProperties(
    val enabled: Boolean = false,
    /** 실측 기반이지만 종별 임계치가 아직 캘리브레이션되지 않아 축제보다 낮고 달력보다 높은 신뢰도를 쓴다. */
    val baseConfidence: Double = 0.7,
    /**
     * 시즌 종료(ENDED) 판정에만 쓰는 신뢰도. 누적 GDD 는 연말까지 임계치를 초과한 채 남으므로,
     * 달력(peakda.timing.calendar.base-confidence)보다 낮게 둬서 시즌이 끝나면 달력이 다음
     * 시즌을 안내하게 한다.
     */
    val endedConfidence: Double = 0.35,
    /** 명소별 관측지점 매핑이 붙기 전까지 모든 명소에 적용할 기본 관측지점. */
    val defaultStationId: String = "",
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
