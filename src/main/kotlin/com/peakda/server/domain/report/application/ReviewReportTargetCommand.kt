package com.peakda.server.domain.report.application

import com.peakda.server.domain.report.entity.ReportTargetType

data class ReviewReportTargetCommand(
    val adminId: Long,
    val targetType: ReportTargetType,
    val targetId: Long,
    val action: ReportReviewAction,
    val memo: String?,
)
