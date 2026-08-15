package com.peakda.server.domain.user.presentation.response

import com.peakda.server.domain.spot.presentation.response.SpotRecordSummaryResponse
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "타인 프로필 조회 (SCR-024h/i)")
data class UserProfileResponse(
    @field:Schema(description = "사용자 PK", example = "42")
    val userId: Long,

    @field:Schema(description = "닉네임", example = "벚꽃러버")
    val nickname: String,

    @field:Schema(description = "프로필 이미지 URL (없으면 null)", example = "https://cdn.peakda.com/profile-images/42/main.jpg")
    val profileImageUrl: String?,

    @field:Schema(description = "통계")
    val stats: Stats,

    @field:Schema(description = "관심 꽃 카테고리 (읽기전용)")
    val favoriteCategories: FavoriteCategoryResponse,

    @field:Schema(description = "최근 게시 기록 그리드 미리보기 (상위 6건, 더보기는 스팟 기록 리스트 API로)")
    val recordPreview: List<SpotRecordSummaryResponse>,

    @field:Schema(
        description = "현재 로그인 사용자가 이 사용자를 팔로우 중인지 여부. 본인 프로필이면 항상 false.",
        example = "true",
    )
    val following: Boolean,

    @field:Schema(
        description = "현재 로그인 사용자가 이 사용자를 차단 중인지 여부. 본인 프로필이면 항상 false.",
        example = "false",
    )
    val blocked: Boolean,
) {
    @Schema(description = "프로필 통계")
    data class Stats(
        @field:Schema(description = "게시된 기록 수", example = "24")
        val recordCount: Long,

        @field:Schema(description = "팔로워 수", example = "1280")
        val followerCount: Long,

        @field:Schema(description = "팔로잉 수", example = "312")
        val followingCount: Long,
    )
}
