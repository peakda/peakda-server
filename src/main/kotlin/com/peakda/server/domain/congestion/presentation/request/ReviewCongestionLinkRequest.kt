package com.peakda.server.domain.congestion.presentation.request

import com.peakda.server.domain.congestion.application.CongestionLinkReviewAction
import com.peakda.server.domain.congestion.application.ReviewCongestionLinkCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

@Schema(description = "혼잡도 연결 검토 요청")
data class ReviewCongestionLinkRequest(
    @field:NotNull
    @field:Schema(description = "검토 결정", example = "CONFIRM")
    val action: CongestionLinkReviewAction?,

    @field:Positive
    @field:Schema(
        description = "CONFIRM 시 연결할 명소 id. 비우면 제안된 명소로 확정한다. REJECT 에서는 무시한다.",
        example = "501",
        nullable = true,
    )
    val attractionId: Long? = null,
) {
    fun toCommand() = ReviewCongestionLinkCommand(
        action = requireNotNull(action),
        attractionId = attractionId,
    )
}
