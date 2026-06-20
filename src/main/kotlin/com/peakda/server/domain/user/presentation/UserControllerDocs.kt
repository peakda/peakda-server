package com.peakda.server.domain.user.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.user.presentation.request.FavoriteCategoryUpdateRequest
import com.peakda.server.domain.user.presentation.response.FavoriteCategoryResponse
import com.peakda.server.domain.user.presentation.response.ProfileImageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Encoding
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.multipart.MultipartFile
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody

@Tag(name = "User", description = "사용자 프로필 관리 API")
interface UserControllerDocs {

    @Operation(
        summary = "프로필 이미지 업로드",
        description = "현재 로그인한 사용자의 프로필 이미지를 업로드한다. " +
            "서버에서 thumbnail(128x128), main(512x512) 사이즈로 리사이즈 후 스토리지에 저장한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
        requestBody = SwaggerRequestBody(
            required = true,
            content = [
                Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = Schema(implementation = ProfileImageUploadForm::class),
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
        ErrorCode.RESOURCE_NOT_FOUND,
    )
    @PostMapping(
        "/me/profile-image",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
    )
    fun uploadProfileImage(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @RequestPart("image") image: MultipartFile,
    ): ResponseEntity<ApiResponse<ProfileImageResponse>>

    @Operation(
        summary = "프로필 이미지 삭제",
        description = "현재 로그인한 사용자의 프로필 이미지를 삭제한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(ErrorCode.UNAUTHORIZED, ErrorCode.RESOURCE_NOT_FOUND, ErrorCode.STORAGE_DELETE_FAILED)
    @DeleteMapping("/me/profile-image")
    fun deleteProfileImage(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
    ): ResponseEntity<ApiResponse<Unit>>

    @Operation(
        summary = "관심 꽃 카테고리 수정",
        description = "현재 로그인한 사용자의 관심 꽃 카테고리를 전체 교체한다. 최소 1개 이상 선택해야 한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(ErrorCode.INVALID_REQUEST, ErrorCode.UNAUTHORIZED)
    @PutMapping("/me/favorite-categories")
    fun updateFavoriteCategories(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Valid @RequestBody request: FavoriteCategoryUpdateRequest,
    ): ResponseEntity<ApiResponse<FavoriteCategoryResponse>>

    @Operation(
        summary = "계정 탈퇴",
        description = "현재 로그인한 사용자의 계정을 탈퇴한다. 본인의 기록·찜·팔로우가 모두 삭제되고 " +
            "계정은 비활성화·익명화되며 복구할 수 없다. 처리 후 인증 쿠키가 만료된다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(ErrorCode.UNAUTHORIZED, ErrorCode.RESOURCE_NOT_FOUND)
    @DeleteMapping("/me")
    fun withdraw(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Parameter(hidden = true)
        response: HttpServletResponse,
    ): ResponseEntity<ApiResponse<Unit>>

    @Schema(description = "프로필 이미지 업로드 multipart form")
    data class ProfileImageUploadForm(
        @field:Schema(type = "string", format = "binary", description = "업로드할 이미지 파일 (jpeg/png/webp, 최대 5MB)")
        val image: MultipartFile,
    )
}
