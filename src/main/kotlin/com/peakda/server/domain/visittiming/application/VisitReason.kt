package com.peakda.server.domain.visittiming.application

/** 추천일을 고른 이유. 문장 조합은 클라이언트가 한다. */
enum class VisitReason {
    /** 추천일이 개화 절정 기간 안이다. */
    BLOOM_PEAK,

    /** 보여주는 기간 중 집중률이 가장 낮은 날이다. */
    LEAST_CROWDED,

    /** 혼잡 등급이 여유다. */
    LOW_CONGESTION,

    /** 맑거나 구름 조금이고 강수확률이 낮다. */
    CLEAR_WEATHER,

    /** 주변 축제가 그날 열리고 있다. */
    FESTIVAL_ONGOING,
}
