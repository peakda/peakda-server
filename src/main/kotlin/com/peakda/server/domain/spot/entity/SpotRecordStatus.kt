package com.peakda.server.domain.spot.entity

/**
 * 작성자 목록은 요청한 상태로 필터링하므로 HIDDEN 탭 조회가 가능하다.
 * 상세 조회에서는 작성자가 자기 HIDDEN 기록을 볼 수 있고, 다른 사용자는 404 응답을 받는다.
 */
enum class SpotRecordStatus {
    DRAFT,
    PUBLISHED,
    HIDDEN,
}
