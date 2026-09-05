package com.peakda.server.domain.spot.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.presentation.request.CreateSpotRecordRequest
import com.peakda.server.domain.spot.presentation.request.UpdateSpotRecordRequest
import com.peakda.server.domain.spot.presentation.response.SpotRecordPhotoUploadResponse
import com.peakda.server.domain.spot.presentation.response.SpotRecordResponse
import com.peakda.server.domain.spot.presentation.response.SpotRecordSummaryResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Encoding
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.bind.annotation.RequestBody as SpringRequestBody

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

    @Operation(
        summary = "스팟 기록 생성 (DRAFT 또는 PUBLISHED)",
        description = "사용자에게 이미 DRAFT 가 있으면 같은 행을 덮어쓰거나 promote 한다. " +
            "PUBLISHED 상태로 생성하려면 visitedDate, bloomStage, plantIds(≥1), photoKeys(1~5) 가 모두 채워져 있어야 한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.SPOT_NOT_FOUND,
        ErrorCode.ATTRACTION_NOT_FOUND,
        ErrorCode.PLANT_NOT_FOUND,
        ErrorCode.PLANT_INACTIVE,
        ErrorCode.SPOT_RECORD_INVALID_STATUS,
    )
    @PostMapping
    fun create(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Valid @SpringRequestBody request: CreateSpotRecordRequest,
    ): ResponseEntity<ApiResponse<SpotRecordResponse>>

    @Operation(
        summary = "스팟 기록 부분 수정",
        description = "본인 기록만 수정 가능. plantIds/photoKeys 는 제공 시 전체를 교체하며 빠진 사진 key 는 스토리지에서도 정리한다. " +
            "PUBLISHED 기록도 수정할 수 있으나 게시 필수 항목을 비울 수는 없다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.SPOT_RECORD_NOT_FOUND,
        ErrorCode.SPOT_RECORD_FORBIDDEN,
        ErrorCode.PLANT_NOT_FOUND,
        ErrorCode.PLANT_INACTIVE,
        ErrorCode.SPOT_RECORD_INVALID_STATUS,
    )
    @PatchMapping("/{id}")
    fun update(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @PathVariable("id") id: Long,
        @Valid @SpringRequestBody request: UpdateSpotRecordRequest,
    ): ResponseEntity<ApiResponse<SpotRecordResponse>>

    @Operation(
        summary = "스팟 기록 게시 전이 (DRAFT → PUBLISHED)",
        description = "DRAFT 상태의 기록을 PUBLISHED 로 명시 전이한다. 필수 항목 미충족 시 400.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.UNAUTHORIZED,
        ErrorCode.SPOT_RECORD_NOT_FOUND,
        ErrorCode.SPOT_RECORD_FORBIDDEN,
        ErrorCode.SPOT_RECORD_INVALID_STATUS,
    )
    @PostMapping("/{id}/publish")
    fun publish(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @PathVariable("id") id: Long,
    ): ResponseEntity<ApiResponse<SpotRecordResponse>>

    @Operation(
        summary = "스팟 기록 삭제",
        description = "본인 기록만 삭제 가능. 첨부 사진도 스토리지에서 함께 제거된다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.UNAUTHORIZED,
        ErrorCode.SPOT_RECORD_NOT_FOUND,
        ErrorCode.SPOT_RECORD_FORBIDDEN,
        ErrorCode.STORAGE_DELETE_FAILED,
    )
    @DeleteMapping("/{id}")
    fun delete(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @PathVariable("id") id: Long,
    ): ResponseEntity<ApiResponse<Unit>>

    @Operation(summary = "스팟 기록 상세 조회", security = [SecurityRequirement(name = "accessTokenCookie")])
    @ApiErrorResponses(ErrorCode.UNAUTHORIZED, ErrorCode.SPOT_RECORD_NOT_FOUND)
    @GetMapping("/{id}")
    fun get(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @PathVariable("id") id: Long,
    ): ResponseEntity<ApiResponse<SpotRecordResponse>>

    @Operation(
        summary = "스팟별 기록 리스트",
        description = "특정 스팟의 모든 기록(DRAFT 포함)을 페이지 단위로 조회한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(ErrorCode.UNAUTHORIZED)
    @GetMapping
    fun listBySpot(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @RequestParam("spotId") spotId: Long,
        @Valid @ModelAttribute pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<SpotRecordSummaryResponse>>>

    @Operation(
        summary = "본인 기록 리스트",
        description = "본인 기록을 status 로 필터하여 페이지 단위로 조회한다 (DRAFT 는 사용자당 최대 1건).",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(ErrorCode.UNAUTHORIZED)
    @GetMapping("/me")
    fun listMine(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @RequestParam("status") status: SpotRecordStatus,
        @Valid @ModelAttribute pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<SpotRecordSummaryResponse>>>

    @Schema(description = "스팟 기록 사진 업로드 multipart form")
    data class SpotRecordPhotoUploadForm(
        @field:ArraySchema(
            schema = Schema(type = "string", format = "binary"),
            arraySchema = Schema(description = "업로드할 이미지 파일들 (jpeg/png/webp, 1~5장, 단일 파일 최대 10MB)"),
            minItems = 1,
            maxItems = 5,
        )
        val images: List<MultipartFile>,
    )
}
