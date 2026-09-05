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
 * [gdd] 는 배치에서는 실측 누적값, 캘린더 예보 범위에서는 날짜별 예측 누적값으로 채운다.
 * [observation] 은 같은 관측지점 권역의 기상청 직접 관측이며, 관측지가 없는 권역에서는 null 이다.
 */
data class BloomEstimationContext(
    val attraction: Attraction,
    val category: BloomCategory,
    val baseDate: LocalDate,
    val festivals: List<Festival> = emptyList(),
    val gdd: GddSnapshot? = null,
    /** 같은 관측지점 권역의 기상청 개화 관측. 관측지가 없는 권역에서는 null 이다. */
    val observation: ObservationSnapshot? = null,
)
