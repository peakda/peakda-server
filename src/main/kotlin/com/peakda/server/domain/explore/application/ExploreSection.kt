package com.peakda.server.domain.explore.application

import com.peakda.server.domain.seasonal.entity.BloomStatus

/** 탐색 화면의 스팟 섹션. [BloomStatus] 전체를 노출하지 않기 위한 화이트리스트다. */
enum class ExploreSection(val status: BloomStatus) {
    PEAK_NOW(BloomStatus.PEAK),
    NEXT_WEEK(BloomStatus.STARTED),
}
