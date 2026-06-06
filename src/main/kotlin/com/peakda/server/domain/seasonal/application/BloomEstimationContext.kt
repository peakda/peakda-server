package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.attraction.entity.Attraction
import com.peakda.server.domain.festival.entity.Festival
import com.peakda.server.domain.seasonal.entity.BloomCategory
import java.time.LocalDate

/**
 * 추정기 입력 컨텍스트. 호출자(배치 잡·캘린더 API)가 필요한 원천 데이터를 모아 구성하며,
 * 추정기는 부수효과 없이 이 컨텍스트만으로 산출한다.
 *
 * [festivals] 는 좌표를 가진 후보 축제 목록으로, 축제 추정기가 카테고리·근접·기간으로 직접 필터한다.
 */
data class BloomEstimationContext(
    val attraction: Attraction,
    val category: BloomCategory,
    val baseDate: LocalDate,
    val festivals: List<Festival> = emptyList(),
)
