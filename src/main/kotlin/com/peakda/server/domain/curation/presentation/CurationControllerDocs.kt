package com.peakda.server.domain.curation.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.domain.curation.presentation.response.CurationCardResponse
import com.peakda.server.domain.curation.presentation.response.CurationDetailResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Curation", description = "큐레이션 콘텐츠 API")
interface CurationControllerDocs {

    @Operation(
        summary = "발행 큐레이션 목록",
        description = "발행된 주차 단위 큐레이션을 최신 주차순으로 조회한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(ErrorCode.INVALID_REQUEST, ErrorCode.UNAUTHORIZED)
    @GetMapping
    fun list(
        @Valid @ModelAttribute pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<CurationCardResponse>>>

    @Operation(
        summary = "발행 큐레이션 상세",
        description = "발행된 큐레이션의 챕터와 추천 카드를 조회한다. " +
            "lat·lng를 모두 전달하면 연결 스팟까지의 거리를 계산한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(ErrorCode.INVALID_REQUEST, ErrorCode.UNAUTHORIZED, ErrorCode.CURATION_NOT_FOUND)
    @GetMapping("/{id}")
    fun detail(
        @Parameter(description = "큐레이션 id", example = "101")
        @PathVariable("id") id: Long,
        @Parameter(description = "거리 계산 기준 위도", example = "37.5665")
        @RequestParam(name = "lat", required = false) lat: Double?,
        @Parameter(description = "거리 계산 기준 경도", example = "126.9780")
        @RequestParam(name = "lng", required = false) lng: Double?,
    ): ResponseEntity<ApiResponse<CurationDetailResponse>>
}
