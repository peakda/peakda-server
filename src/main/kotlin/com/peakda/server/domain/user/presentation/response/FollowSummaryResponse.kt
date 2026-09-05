package com.peakda.server.domain.user.presentation.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "사용자 팔로우 통계 요약 (유저 프로필 헤더용)")
data class FollowSummaryResponse(
    @field:Schema(description = "대상 사용자 PK", example = "42")
    val userId: Long,

    @field:Schema(description = "팔로워 수 (이 사용자를 팔로우하는 사람 수)", example = "1280")
    val followerCount: Long,

    @field:Schema(description = "팔로잉 수 (이 사용자가 팔로우하는 사람 수)", example = "312")
    val followingCount: Long,

    @field:Schema(
        description = "현재 로그인 사용자가 이 사용자를 팔로우 중인지 여부. 본인 프로필이면 항상 false.",
        example = "true",
    )
    val following: Boolean,
)
