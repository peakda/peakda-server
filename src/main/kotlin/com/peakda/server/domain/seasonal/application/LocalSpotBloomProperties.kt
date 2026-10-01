package com.peakda.server.domain.seasonal.application

import org.springframework.boot.context.properties.ConfigurationProperties

/** 동네 스팟의 지도 핀·프리뷰·검색 뱃지에 사용하는 관측 유효 기간. */
@ConfigurationProperties(prefix = "peakda.timing.local-spot")
data class LocalSpotBloomProperties(
    /** 관측일로부터 이 일수를 넘기면 현재 상태의 근거로 쓰지 않는다. 경계일은 포함한다. */
    val maxAgeDays: Long = 30,
)
