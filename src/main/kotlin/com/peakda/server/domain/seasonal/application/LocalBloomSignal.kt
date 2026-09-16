package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus

/**
 * 동네형(LOCAL) Spot 의 카테고리 1건에 대한 최근 관측 신호.
 *
 * [LocalSpotBloomResolver] 가 카테고리마다 가장 최근 게시 기록 한 건만 환산해 만든다.
 * 지도 슬롯·프리뷰 뱃지·검색 뱃지가 각자의 응답 타입으로 옮겨 담는다.
 */
data class LocalBloomSignal(
    val category: BloomCategory,
    val status: BloomStatus,
)
