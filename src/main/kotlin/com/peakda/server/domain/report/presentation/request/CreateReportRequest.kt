package com.peakda.server.domain.report.presentation.request

import com.peakda.server.domain.report.entity.ReportReason
import com.peakda.server.domain.report.entity.ReportTargetType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "UGC 신고 생성 요청")
data class CreateReportRequest(
    @field:Schema(description = "신고 대상 종류 (V1 은 SPOT_RECORD 만 지원)", example = "SPOT_RECORD")
    val targetType: ReportTargetType,

    @field:Schema(description = "신고 대상 id (targetType=SPOT_RECORD 면 스팟 기록 id)", example = "1024")
    val targetId: Long,

    @field:Schema(description = "신고 사유", example = "SPAM")
    val reason: ReportReason,

    @field:Size(max = 500)
    @field:Schema(description = "상세 사유 (선택, 최대 500자)", example = "같은 내용을 반복 게시합니다.")
    val detail: String? = null,
)
