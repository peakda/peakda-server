package com.peakda.server.domain.user.presentation.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "프로필 이미지 업로드 결과")
data class ProfileImageResponse(
    @field:Schema(
        description = "대표 이미지 URL (User.profileImageUrl 에 저장된 값)",
        example = "https://cdn.example.com/profile-images/1/main.jpg",
    )
    val profileImageUrl: String,
    @field:Schema(
        description = "사이즈 variant 별 URL 매핑",
        example = "{\"thumbnail\":\"https://cdn.example.com/profile-images/1/thumbnail.jpg\",\"main\":\"https://cdn.example.com/profile-images/1/main.jpg\"}",
    )
    val variants: Map<String, String>,
)
