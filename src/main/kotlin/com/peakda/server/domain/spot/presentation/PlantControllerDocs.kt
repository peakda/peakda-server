package com.peakda.server.domain.spot.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.spot.presentation.request.SuggestPlantRequest
import com.peakda.server.domain.spot.presentation.response.PlantResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Plant", description = "식물 마스터/제안 API")
interface PlantControllerDocs {

    @Operation(
        summary = "활성 식물 마스터 리스트",
        description = "ACTIVE 상태의 식물을 sortOrder 오름차순으로 반환한다. (Step2 식물 칩용)",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(ErrorCode.UNAUTHORIZED)
    @GetMapping
    fun list(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
    ): ResponseEntity<ApiResponse<List<PlantResponse>>>

    @Operation(
        summary = "식물 검색",
        description = "ACTIVE 식물을 이름 contains (대소문자 무시) 로 검색한다. 빈 키워드는 빈 결과를 반환.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(ErrorCode.UNAUTHORIZED)
    @GetMapping("/search")
    fun search(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @RequestParam("keyword") keyword: String,
    ): ResponseEntity<ApiResponse<List<PlantResponse>>>

    @Operation(
        summary = "식물 제안 (PENDING)",
        description = "검색에서 찾지 못한 식물 이름을 사용자가 제안한다. 동일 이름이 이미 있으면 409, " +
            "한 사용자가 최근 24시간 내 5건을 초과하면 429. 관리자 승인을 거쳐 ACTIVE 로 전환된다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.PLANT_SUGGESTION_DUPLICATE,
        ErrorCode.PLANT_SUGGESTION_RATE_LIMIT,
    )
    @PostMapping("/suggestions")
    fun suggest(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Valid @RequestBody request: SuggestPlantRequest,
    ): ResponseEntity<ApiResponse<PlantResponse>>
}
