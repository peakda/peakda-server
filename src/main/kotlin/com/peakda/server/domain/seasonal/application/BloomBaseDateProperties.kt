package com.peakda.server.domain.seasonal.application

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 명소 개화 산출물의 유효 기간 튜닝값. 운영 중 yml 로 조정한다.
 */
@ConfigurationProperties(prefix = "peakda.timing.base-date")
data class BloomBaseDateProperties(
    /**
     * 산출일이 오늘로부터 이보다 오래되면 "현재 상태"로 쓰지 않는다.
     *
     * 산출 잡은 매일 1회 돌므로 정상 상태의 산출일은 오늘 또는 어제다. 기본값 2 는 잡이 한 번 걸러도
     * 버티되, 그 이상 밀리면 철 지난 상태를 현재로 내보내는 대신 노출을 멈추게 한다.
     */
    val maxAgeDays: Long = 2,
)
