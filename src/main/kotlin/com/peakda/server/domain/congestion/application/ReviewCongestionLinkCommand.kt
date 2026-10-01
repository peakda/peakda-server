package com.peakda.server.domain.congestion.application

data class ReviewCongestionLinkCommand(
    val action: CongestionLinkReviewAction,
    /** CONFIRM 에서 연결할 명소. 비우면 매칭 잡이 채워 둔 명소로 확정한다. */
    val attractionId: Long?,
)
