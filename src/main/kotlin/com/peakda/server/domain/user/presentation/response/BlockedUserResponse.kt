package com.peakda.server.domain.user.presentation.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "차단한 사용자 (차단 목록 항목)")
data class BlockedUserResponse(
    @field:Schema(description = "사용자 PK", example = "42")
    val userId: Long,

    @field:Schema(description = "닉네임", example = "불편러")
    val nickname: String,

    @field:Schema(description = "프로필 이미지 URL (없으면 null)", example = "https://cdn.peakda.com/profile-images/42/main.jpg")
    val profileImageUrl: String?,

    @field:Schema(description = "차단한 시각", example = "2026-07-03T09:41:00Z")
    val blockedAt: Instant,
)
