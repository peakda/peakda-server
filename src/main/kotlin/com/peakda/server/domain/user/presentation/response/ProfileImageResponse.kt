package com.peakda.server.domain.user.presentation.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "프로필 이미지 업로드 결과")
data class ProfileImageResponse(
    @field:Schema(
        description = "대표 이미지 presigned URL. 즉시 표시용 (만료 있음)",
        example = "https://t3.storageapi.dev/peakda-bucket/profile-images/1/main.jpg?X-Amz-Signature=...",
    )
    val profileImageUrl: String,
    @field:Schema(
        description = "대표 이미지의 저장소 key. " +
            "회원가입 임시 업로드의 경우 이 값을 /signup/complete 의 profileImageUrl 로 그대로 전달해야 한다.",
        example = "temp/signup/42/main.jpg",
    )
    val profileImageKey: String,
    @field:Schema(
        description = "사이즈 variant 별 presigned URL 매핑",
        example = "{\"thumbnail\":\"https://...thumbnail.jpg?X-Amz-Signature=...\",\"main\":\"https://...main.jpg?X-Amz-Signature=...\"}",
    )
    val variants: Map<String, String>,
)
