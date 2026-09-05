package com.peakda.server.domain.report.entity

/** 신고 대상 종류. V1 은 스팟 기록(게시글)만 신고 가능하며, 확장은 이 enum 에 값만 추가한다. */
enum class ReportTargetType {
    SPOT_RECORD,
}
