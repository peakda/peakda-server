package com.peakda.server.domain.report.presentation.request

import com.peakda.server.domain.report.application.ReportReviewAction
import com.peakda.server.domain.report.application.ReviewReportTargetCommand
import com.peakda.server.domain.report.entity.ReportTargetType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "신고 대상 심사 요청")
data class ReviewReportTargetRequest(
    @field:Schema(description = "심사 조치", example = "RESOLVE_HIDE")
    val action: ReportReviewAction,

    @field:Size(max = 500)
    @field:Schema(description = "심사 메모 (선택, 최대 500자)", example = "운영 정책 위반 게시글 숨김")
    val memo: String? = null,
) {
    fun toCommand(adminId: Long, targetType: ReportTargetType, targetId: Long): ReviewReportTargetCommand =
        ReviewReportTargetCommand(
            adminId = adminId,
            targetType = targetType,
            targetId = targetId,
            action = action,
            memo = memo,
        )
}
