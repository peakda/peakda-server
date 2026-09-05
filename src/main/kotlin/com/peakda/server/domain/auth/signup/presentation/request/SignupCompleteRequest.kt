package com.peakda.server.domain.auth.signup.presentation.request

import com.peakda.server.domain.seasonal.entity.BloomCategory
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@Schema(description = "소셜 회원가입 완료 요청")
data class SignupCompleteRequest(
    @field:Schema(
        description = "닉네임. 한글, 영문, 숫자만 허용 (2~10자)",
        example = "peakda",
        minLength = 2,
        maxLength = 10,
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @field:Size(min = 2, max = 10)
    @field:Pattern(regexp = "^[가-힣a-zA-Z0-9]+$")
    val nickname: String,

    @field:Schema(
        description = "프로필 이미지 값. " +
            "(1) 가입 시 임시 업로드 endpoint 로 받은 key (`temp/signup/{sessionId}/main.jpg`) 또는 " +
            "(2) OAuth2 제공자가 준 외부 URL. " +
            "미지정 시 OAuth2 제공자 프로필 이미지가 사용됨.",
        example = "temp/signup/42/main.jpg",
        nullable = true,
    )
    val profileImageUrl: String? = null,

    @field:Schema(
        description = "관심 꽃 카테고리 목록. 최소 1개 이상 선택해야 한다.",
        example = "[\"CHERRY\", \"MAPLE\"]",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @field:NotEmpty
    val favoriteCategories: Set<BloomCategory>,
)
