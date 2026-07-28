package com.peakda.server.domain.report.presentation.response

import com.peakda.server.domain.report.entity.ReportReason
import com.peakda.server.domain.report.entity.ReportTargetType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "신고 대상 심사 상세")
data class ReportTargetDetailResponse(
    @field:Schema(description = "신고 대상 종류", example = "SPOT_RECORD")
    val targetType: ReportTargetType,

    @field:Schema(description = "신고 대상 id", example = "1024")
    val targetId: Long,

    @field:Schema(description = "신고 건수", example = "3")
    val reportCount: Long,

    @field:Schema(description = "사유별 신고 건수")
    val reasonDistribution: Map<ReportReason, Long>,

    @field:Schema(description = "개별 신고 목록")
    val reports: List<ReportReviewItemResponse>,

    @field:Schema(description = "대상 콘텐츠 요약", example = "반복 광고 게시글", nullable = true)
    val targetSummary: String?,

    @field:Schema(description = "대상 콘텐츠 작성자 닉네임", example = "여행자", nullable = true)
    val targetAuthorNickname: String?,

    @field:Schema(description = "대상 존재 여부", example = "true")
    val targetExists: Boolean,
)
