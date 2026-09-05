package com.peakda.server.domain.spot.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.spot.entity.PlantStatus
import com.peakda.server.domain.spot.presentation.request.UpdatePlantRequest
import com.peakda.server.domain.spot.presentation.response.PlantAdminResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Plant Admin", description = "식물 제안 사후 검수 관리자 API")
interface PlantAdminControllerDocs {

    @Operation(
        summary = "식물 검수 목록 조회",
        description = "기본으로 사용자가 제안한 식물을 상태와 관계없이 최신순으로 조회한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
    )
    @GetMapping
    fun list(
        @Parameter(description = "식물 상태", example = "ACTIVE")
        @RequestParam(name = "status", required = false) status: PlantStatus?,
        @Parameter(description = "사용자가 제안한 식물만 조회", example = "true")
        @RequestParam(name = "suggestedOnly", defaultValue = "true") suggestedOnly: Boolean,
        @Valid @ModelAttribute pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<PlantAdminResponse>>>

    @Operation(
        summary = "식물 검수 정보 수정",
        description = "공백이 아닌 이름, 정렬 순서, 상태, 개화 카테고리와 계절 배열을 부분 수정한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
        ErrorCode.PLANT_NOT_FOUND,
        ErrorCode.PLANT_SUGGESTION_DUPLICATE,
    )
    @PatchMapping("/{id}")
    fun update(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Parameter(description = "식물 id", example = "4")
        @PathVariable("id") id: Long,
        @Valid @RequestBody request: UpdatePlantRequest,
    ): ResponseEntity<ApiResponse<PlantAdminResponse>>
}
