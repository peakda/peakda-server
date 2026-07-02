package com.peakda.server.domain.search.presentation.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "사용자 검색 결과 1건")
data class UserSearchItem(
    @field:Schema(description = "사용자 id", example = "42")
    val userId: Long,

    @field:Schema(description = "닉네임", example = "피크다")
    val nickname: String,

    @field:Schema(description = "프로필 이미지 URL (없으면 null)", example = "https://img/profile.jpg")
    val profileImageUrl: String?,
)
