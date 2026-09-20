package com.peakda.server.domain.spot.application

/** [SpotRecordPhotoVariantBackfillService.backfill] 한 번의 실행 결과. */
data class SpotRecordPhotoVariantBackfillResult(
    val scanned: Int,
    val updated: Int,
    val failed: Int,
    val remaining: Long,
)
