package com.peakda.server.domain.user.presentation.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "팔로우 관계의 사용자 정보 (팔로워/팔로잉 목록 항목)")
data class FollowUserResponse(
    @field:Schema(description = "사용자 PK", example = "42")
    val userId: Long,

    @field:Schema(description = "닉네임", example = "벚꽃러버")
    val nickname: String,

    @field:Schema(description = "프로필 이미지 URL (없으면 null)", example = "https://cdn.peakda.com/profile-images/42/main.jpg", nullable = true)
    val profileImageUrl: String?,

    @field:Schema(
        description = "현재 로그인 사용자가 이 사용자를 팔로우 중인지 여부. " +
            "false 이면 '팔로우'(맞팔로우) 버튼, true 이면 '팔로잉' 버튼을 노출한다.",
        example = "false",
    )
    val following: Boolean,

    @field:Schema(description = "팔로우 관계가 생성된 시각", example = "2026-05-29T09:41:00Z")
    val followedAt: Instant,
)
