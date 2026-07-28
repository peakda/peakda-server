package com.peakda.server.domain.spot.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.spot.presentation.request.UpdateSpotRecordStatusRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody

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
}
