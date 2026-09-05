package com.peakda.server.domain.report.entity

/** 신고 사유 (고정 분류). ETC 선택 시 [Report.detail] 로 상세 사유를 받는다. */
enum class ReportReason {
    SPAM,
    INAPPROPRIATE,
    HARASSMENT,
    ETC,
}
