package com.peakda.server.domain.visittiming.application

import com.peakda.server.domain.seasonal.entity.BloomStatus
import java.time.LocalDate

/** 방문 타이밍 판단에 쓰는 대표 개화 상태 (명소 상세의 개화 배너와 같은 추정). */
data class VisitTimingBloom(
    val status: BloomStatus,
    val peakStartDate: LocalDate?,
    val peakEndDate: LocalDate?,
)
