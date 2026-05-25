package com.peakda.server.domain.spot.presentation.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "스팟 기록 사진 업로드 결과")
data class SpotRecordPhotoUploadResponse(
    @field:Schema(description = "업로드된 사진 목록 (업로드 순서 유지)")
    val photos: List<UploadedSpotRecordPhoto>,
) {
    @Schema(description = "업로드된 단일 사진")
    data class UploadedSpotRecordPhoto(
        @field:Schema(
            description = "ObjectStorage 저장 key. 기록 생성 시 photoKeys 로 그대로 전달",
            example = "spot-records/42/2026/05/9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d/main.jpg",
        )
        val objectKey: String,
        @field:Schema(
            description = "즉시 미리보기용 presigned URL (만료 있음, DB 저장 금지)",
            example = "https://t3.storageapi.dev/peakda-bucket/spot-records/42/2026/05/.../main.jpg?X-Amz-Signature=...",
        )
        val previewUrl: String,
    )
}
