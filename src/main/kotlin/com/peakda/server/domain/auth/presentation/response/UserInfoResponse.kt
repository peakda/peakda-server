package com.peakda.server.domain.auth.presentation.response

import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.entity.UserStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "현재 로그인한 사용자 정보")
data class UserInfoResponse(
    @field:Schema(description = "사용자 ID", example = "1")
    val id: Long,
    @field:Schema(description = "이메일", example = "user@example.com", nullable = true)
    val email: String?,
    @field:Schema(description = "닉네임", example = "peakda", nullable = true)
    val nickname: String?,
    @field:Schema(
        description = "프로필 이미지 URL(512px). OAuth2 제공자가 준 외부 URL 그대로이거나, 우리 버킷 객체의 URL",
        example = "https://cdn.peakda.com/profile-images/1/main.jpg",
        nullable = true,
    )
    val profileImageUrl: String?,
    @field:Schema(
        description = "프로필 이미지의 사이즈 variant 별 URL. thumbnail=128px, main=512px. " +
            "외부 OAuth 제공자가 준 이미지는 고를 사이즈가 없어 빈 객체다.",
        example = "{\"thumbnail\":\"https://cdn.peakda.com/profile-images/1/thumbnail.jpg\"," +
            "\"main\":\"https://cdn.peakda.com/profile-images/1/main.jpg\"}",
    )
    val profileImageVariants: Map<String, String> = emptyMap(),
    @field:Schema(description = "사용자 상태", example = "ACTIVE")
    val status: UserStatus,
    @field:Schema(description = "관심 꽃 카테고리 목록", example = "[\"CHERRY\", \"MAPLE\"]")
    val favoriteCategories: List<BloomCategory>,
) {
    companion object {
        fun from(
            user: User,
            profileImageUrl: String?,
            profileImageVariants: Map<String, String>,
            favoriteCategories: Collection<BloomCategory>,
        ): UserInfoResponse {
            return UserInfoResponse(
                id = requireNotNull(user.id),
                email = user.email,
                nickname = user.nickname,
                profileImageUrl = profileImageUrl,
                profileImageVariants = profileImageVariants,
                status = user.status,
                favoriteCategories = favoriteCategories.sortedBy { it.ordinal },
            )
        }
    }
}
