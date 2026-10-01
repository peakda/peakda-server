package com.peakda.server.domain.congestion.entity

/** 연결 근거. */
enum class CongestionMatchType {
    /** 같은 법정동 시군구 안에서 정규화한 이름이 정확히 일치. */
    EXACT,

    /** 행정구역 개편으로 시군구 코드가 사라져, 개편 후 시도 범위에서 정규화한 이름이 정확히 일치. */
    REGION_REFORM,

    /** 같은 시군구 안에서 한쪽 이름이 다른 쪽에 포함됨. 자동 확정하지 않는다. */
    CONTAINS,

    /** 관리자가 직접 지정. */
    MANUAL,
}
