package com.peakda.server.domain.curation.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.domain.curation.presentation.response.UploadedImageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Encoding
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.multipart.MultipartFile

@Tag(name = "Curation Admin", description = "큐레이션 관리자 API")
interface CurationImageAdminControllerDocs {

    @Operation(
        summary = "큐레이션 이미지 업로드",
        description = "큐레이션과 축제 에디토리얼에서 공용으로 사용할 가로형 이미지를 업로드한다. " +
            "응답 objectKey만 저장 API에 전달하고, 만료되는 previewUrl은 DB에 저장하지 않는다. " +
            "단일 파일은 최대 10MB이며 jpeg/png/webp를 지원한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
        requestBody = RequestBody(
            required = true,
            content = [
                Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = Schema(implementation = CurationImageUploadForm::class),
                    encoding = [Encoding(name = "file", contentType = "image/jpeg, image/png, image/webp")],
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
        ErrorCode.FORBIDDEN,
    )
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<ApiResponse<UploadedImageResponse>>

    @Schema(description = "큐레이션 이미지 업로드 multipart form")
    data class CurationImageUploadForm(
        @field:Schema(
            description = "업로드할 이미지 파일(jpeg/png/webp, 최대 10MB)",
            implementation = MultipartFile::class,
        )
        val file: MultipartFile,
    )
}
