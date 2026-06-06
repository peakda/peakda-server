package com.peakda.server.domain.seasonal.entity

/**
 * 명소×카테고리의 개화/단풍 상태.
 *
 * 지도 핀은 앞 3단계(PREPARING/STARTED/PEAK)만 노출하고 ENDED 는 내부 전이 상태로만 사용한다.
 */
enum class BloomStatus {
    PREPARING,
    STARTED,
    PEAK,
    ENDED,
}
