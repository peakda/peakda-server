package com.peakda.server.domain.spot.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.spot.presentation.response.SpotRecordPhotoUploadResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Encoding
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.multipart.MultipartFile

@Tag(name = "Spot Record", description = "스팟 기록 (방문 기록) API")
interface SpotRecordControllerDocs {

    @Operation(
        summary = "스팟 기록 사진 업로드",
        description = "스팟 기록 생성 전에 사진을 미리 업로드한다. " +
            "응답으로 받은 objectKey 를 기록 생성/수정 API 의 photoKeys 로 전달하면 된다. " +
            "한 번에 1~5장, 단일 파일은 최대 10MB.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
        requestBody = RequestBody(
            required = true,
            content = [
                Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = Schema(implementation = SpotRecordPhotoUploadForm::class),
                    encoding = [Encoding(name = "images", contentType = "image/jpeg, image/png, image/webp")],
                ),
            ],
        ),
    )
    @ApiErrorResponses(
        ErrorCode.IMAGE_REQUIRED,
        ErrorCode.INVALID_IMAGE_FORMAT,
        ErrorCode.IMAGE_SIZE_EXCEEDED,
        ErrorCode.IMAGE_PROCESSING_FAILED,
        ErrorCode.SPOT_RECORD_PHOTO_LIMIT,
        ErrorCode.STORAGE_UPLOAD_FAILED,
        ErrorCode.UNAUTHORIZED,
    )
    @PostMapping(
        "/photos",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
    )
    fun uploadPhotos(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @RequestPart("images") images: List<MultipartFile>,
    ): ResponseEntity<ApiResponse<SpotRecordPhotoUploadResponse>>

    @Schema(description = "스팟 기록 사진 업로드 multipart form")
    data class SpotRecordPhotoUploadForm(
        @field:Schema(
            type = "array",
            description = "업로드할 이미지 파일들 (jpeg/png/webp, 1~5장, 단일 파일 최대 10MB)",
            implementation = MultipartFile::class,
        )
        val images: List<MultipartFile>,
    )
}
