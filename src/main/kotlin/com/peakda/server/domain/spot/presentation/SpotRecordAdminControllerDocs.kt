package com.peakda.server.domain.spot.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.spot.presentation.request.UpdateSpotRecordStatusRequest
import com.peakda.server.domain.spot.presentation.response.SpotRecordPhotoBackfillResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Spot Record Admin", description = "스팟 기록 관리자 노출 상태 API")
interface SpotRecordAdminControllerDocs {

    @Operation(
        summary = "스팟 기록 노출 상태 변경",
        description = "스팟 기록을 숨기거나 다시 게시 상태로 복구한다. 물리 삭제는 지원하지 않는다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
        ErrorCode.SPOT_RECORD_NOT_FOUND,
        ErrorCode.SPOT_RECORD_INVALID_STATUS,
    )
    @PatchMapping("/{id}/status")
    fun updateStatus(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Parameter(description = "스팟 기록 id", example = "1024")
        @PathVariable("id") id: Long,
        @Valid @RequestBody request: UpdateSpotRecordStatusRequest,
    ): ResponseEntity<ApiResponse<Unit>>

    @Operation(
        summary = "기록 사진 이미지 variant 백필",
        description = "variant 세트가 늘어나기 전에 올라온 사진의 빠진 이미지를 원본에서 다시 만들어 채운다. " +
            "한 번에 batchSize 장씩 처리하므로 응답의 remaining 이 0 이 될 때까지 반복 호출한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
        ErrorCode.STORAGE_DOWNLOAD_FAILED,
    )
    @PostMapping("/photos/backfill-variants")
    fun backfillPhotoVariants(
        @Parameter(description = "한 번에 처리할 사진 수 (1~200)", example = "50")
        @RequestParam(name = "batchSize", required = false, defaultValue = "50") batchSize: Int,
    ): ResponseEntity<ApiResponse<SpotRecordPhotoBackfillResponse>>
}
