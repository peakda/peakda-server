package com.peakda.server.domain.report.presentation.response

import com.peakda.server.domain.report.entity.ReportReason
import com.peakda.server.domain.report.entity.ReportStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "신고 심사 상세의 개별 신고")
data class ReportReviewItemResponse(
    @field:Schema(description = "신고 id", example = "501")
    val id: Long,

    @field:Schema(description = "신고자 사용자 id", example = "22")
    val reporterId: Long,

    @field:Schema(description = "신고자 닉네임", example = "신고자", nullable = true)
    val reporterNickname: String?,

    @field:Schema(description = "신고 사유", example = "SPAM")
    val reason: ReportReason,

    @field:Schema(description = "상세 신고 사유", example = "같은 광고를 반복 게시합니다.", nullable = true)
    val detail: String?,

    @field:Schema(description = "신고 심사 상태", example = "PENDING")
    val status: ReportStatus,

    @field:Schema(description = "신고 접수 시각", example = "2026-07-28T09:30:00Z")
    val createdAt: Instant,

    @field:Schema(description = "심사한 관리자 사용자 id", example = "7", nullable = true)
    val reviewedBy: Long?,

    @field:Schema(description = "심사 완료 시각", example = "2026-07-28T10:00:00Z", nullable = true)
    val reviewedAt: Instant?,

    @field:Schema(description = "심사 메모", example = "정책 위반 확인", nullable = true)
    val reviewMemo: String?,
)
