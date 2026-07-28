package com.peakda.server.domain.report.application

internal data class ReportTargetContext(
    val summary: String?,
    val authorNickname: String?,
    val exists: Boolean,
) {
    companion object {
        fun missing(): ReportTargetContext = ReportTargetContext(
            summary = null,
            authorNickname = null,
            exists = false,
        )
    }
}
