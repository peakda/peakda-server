package com.peakda.server.domain.auth.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.common.security.principal.SignupSessionPrincipal
import com.peakda.server.domain.auth.presentation.response.UserInfoResponse
import com.peakda.server.domain.auth.signup.presentation.request.SignupCompleteRequest
import com.peakda.server.domain.auth.signup.presentation.response.NicknameCheckResponse
import com.peakda.server.domain.user.presentation.response.ProfileImageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Encoding
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.multipart.MultipartFile
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody

@Tag(name = "Auth", description = "로그인, 회원가입, 토큰 관리 API")
interface AuthControllerDocs {

    @Operation(
        summary = "내 정보 조회",
        description = "access-token 쿠키로 현재 로그인한 사용자의 정보를 조회합니다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(ErrorCode.UNAUTHORIZED, ErrorCode.RESOURCE_NOT_FOUND)
    @GetMapping("/me")
    fun getCurrentUser(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
    ): ResponseEntity<ApiResponse<UserInfoResponse>>

    @Operation(
        summary = "회원가입 닉네임 중복 확인",
        description = "소셜 로그인 후 발급된 signup-token 쿠키로 닉네임 사용 가능 여부를 확인합니다.",
        security = [SecurityRequirement(name = "signupTokenCookie")],
    )
    @ApiErrorResponses(ErrorCode.NICKNAME_INVALID, ErrorCode.UNAUTHORIZED)
    @GetMapping("/signup/nickname/check")
    fun checkNickname(
        @Parameter(description = "2~10자의 한글, 영문, 숫자 닉네임", example = "peakda")
        @RequestParam value: String,
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: SignupSessionPrincipal,
    ): ResponseEntity<ApiResponse<NicknameCheckResponse>>

    @Operation(
        summary = "회원가입 임시 프로필 이미지 업로드",
        description = "signup-token 쿠키로 인증된 사용자가 회원가입 완료 전에 프로필 이미지를 업로드한다. " +
            "이미지는 temp 영역에 저장되며, 가입 완료 시 정식 영역으로 이관된다. " +
            "응답으로 받은 main URL을 /signup/complete 의 profileImageUrl 로 전달해야 한다.",
        security = [SecurityRequirement(name = "signupTokenCookie")],
        requestBody = SwaggerRequestBody(
            required = true,
            content = [
                Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = Schema(implementation = SignupProfileImageUploadForm::class),
                    encoding = [Encoding(name = "image", contentType = "image/jpeg, image/png, image/webp")],
                ),
            ],
        ),
    )
    @ApiErrorResponses(
        ErrorCode.IMAGE_REQUIRED,
        ErrorCode.INVALID_IMAGE_FORMAT,
        ErrorCode.IMAGE_SIZE_EXCEEDED,
        ErrorCode.IMAGE_PROCESSING_FAILED,
        ErrorCode.STORAGE_UPLOAD_FAILED,
        ErrorCode.UNAUTHORIZED,
    )
    @PostMapping(
        "/signup/profile-image",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
    )
    fun uploadSignupProfileImage(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: SignupSessionPrincipal,
        @RequestPart("image") image: MultipartFile,
    ): ResponseEntity<ApiResponse<ProfileImageResponse>>

    @Schema(description = "회원가입 임시 프로필 이미지 업로드 multipart form")
    data class SignupProfileImageUploadForm(
        @field:Schema(type = "string", format = "binary", description = "업로드할 이미지 파일 (jpeg/png/webp, 최대 5MB)")
        val image: MultipartFile,
    )

    @Operation(
        summary = "소셜 회원가입 완료",
        description = "signup-token 쿠키와 닉네임으로 회원가입을 완료하고 access-token, refresh-token 쿠키를 발급합니다.",
        security = [SecurityRequirement(name = "signupTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.NICKNAME_INVALID,
        ErrorCode.NICKNAME_DUPLICATED,
        ErrorCode.UNAUTHORIZED,
    )
    @PostMapping("/signup/complete")
    fun completeSignup(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: SignupSessionPrincipal,
        @Valid @RequestBody request: SignupCompleteRequest,
        @Parameter(hidden = true)
        response: HttpServletResponse,
    ): ResponseEntity<ApiResponse<Unit>>

    @Operation(
        summary = "토큰 재발급",
        description = "refresh-token 쿠키로 access-token, refresh-token 쿠키를 재발급합니다.",
        security = [SecurityRequirement(name = "refreshTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.REFRESH_TOKEN_EXPIRED,
        ErrorCode.REFRESH_TOKEN_INVALID,
        ErrorCode.RESOURCE_NOT_FOUND,
    )
    @PostMapping("/refresh")
    fun refresh(
        @Parameter(hidden = true)
        request: HttpServletRequest,
        @Parameter(hidden = true)
        response: HttpServletResponse,
    ): ResponseEntity<ApiResponse<Unit>>

    @Operation(
        summary = "로그아웃",
        description = "서버에 저장된 refresh token을 삭제하고 access-token, refresh-token 쿠키를 만료시킵니다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(ErrorCode.UNAUTHORIZED)
    @PostMapping("/logout")
    fun logout(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Parameter(hidden = true)
        response: HttpServletResponse,
    ): ResponseEntity<ApiResponse<Unit>>
}
