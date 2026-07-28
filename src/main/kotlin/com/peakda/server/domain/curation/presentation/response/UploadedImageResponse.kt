package com.peakda.server.domain.curation.presentation.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "큐레이션 이미지 업로드 결과")
data class UploadedImageResponse(
    @field:Schema(
        description = "main 이미지 object key. 큐레이션 또는 축제 에디토리얼 저장 요청에 그대로 전달한다.",
        example = "curations/2026-07/9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d/main.jpg",
    )
    val objectKey: String,

    @field:Schema(
        description = "화면 미리보기용 presigned URL. 만료되므로 DB에 저장하지 않는다.",
        example = "https://storage.example.com/curations/2026-07/.../main.jpg?X-Amz-Signature=...",
    )
    val previewUrl: String,
)
