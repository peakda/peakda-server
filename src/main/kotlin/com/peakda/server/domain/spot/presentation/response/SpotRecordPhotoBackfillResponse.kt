package com.peakda.server.domain.spot.presentation.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "기록 사진 variant 백필 실행 결과")
data class SpotRecordPhotoBackfillResponse(
    @field:Schema(description = "이번 배치에서 처리를 시도한 사진 수", example = "50")
    val scanned: Int,
    @field:Schema(description = "variant 를 새로 만들어 채운 사진 수", example = "48")
    val updated: Int,
    @field:Schema(description = "원본을 읽지 못하는 등의 이유로 건너뛴 사진 수", example = "2")
    val failed: Int,
    @field:Schema(description = "아직 백필되지 않고 남은 사진 수. 0 이 되면 완료", example = "120")
    val remaining: Long,
)
