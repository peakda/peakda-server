package com.peakda.server.domain.explore.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.explore.application.ExploreSection
import com.peakda.server.domain.explore.presentation.response.ExploreFestivalListResponse
import com.peakda.server.domain.explore.presentation.response.ExploreResponse
import com.peakda.server.domain.explore.presentation.response.ExploreResponse.ExploreSpotItem
import com.peakda.server.domain.seasonal.entity.BloomCategory
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Explore", description = "탐색 큐레이션 API")
interface ExploreControllerDocs {

    @Operation(
        summary = "탐색 큐레이션 조회",
        description = "지금이 절정(최신 산출일 status=PEAK), " +
            "다음 주에 가면 좋을 곳(status=STARTED, 기본 5건), " +
            "지금 열리는 축제(오늘 진행 중인 꽃축제, 종료 임박순), " +
            "큐레이션 카드(발행된 최신 주차순)를 조회한다. " +
            "개화 추정이 없으면 두 스팟 섹션은 빈 목록이다. " +
            "명소 노출 여부는 개화 추정과 다른 도메인에서 관리하므로 " +
            "비노출 명소가 포함된 페이지는 응답 카드 수가 페이지 명소 수보다 적을 수 있다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(ErrorCode.INVALID_REQUEST, ErrorCode.UNAUTHORIZED)
    @GetMapping
    fun explore(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Parameter(description = "꽃 카테고리 필터", example = "CHERRY")
        @RequestParam(name = "category", required = false) category: BloomCategory?,
    ): ResponseEntity<ApiResponse<ExploreResponse>>

    @Operation(
        summary = "탐색 스팟 섹션 전체 보기",
        description = "PEAK_NOW는 최신 산출일 status=PEAK, NEXT_WEEK는 status=STARTED 명소를 페이징한다. " +
            "개화 추정이 없으면 빈 목록이다. 비노출 명소가 페이지에 포함되면 " +
            "content 건수가 totalElements 기준 건수보다 적을 수 있다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(ErrorCode.INVALID_REQUEST, ErrorCode.UNAUTHORIZED)
    @GetMapping("/spots")
    fun spots(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Parameter(description = "탐색 스팟 섹션", example = "PEAK_NOW")
        @RequestParam(name = "section") section: ExploreSection,
        @Parameter(description = "꽃 카테고리 필터", example = "CHERRY")
        @RequestParam(name = "category", required = false) category: BloomCategory?,
        @Valid @ModelAttribute pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<ExploreSpotItem>>>

    @Operation(
        summary = "진행 중 꽃축제 전체 보기",
        description = "오늘 진행 중인 축제 후보에서 꽃 이름이 매칭되는 축제를 " +
            "종료 임박순으로 전량 조회한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(ErrorCode.INVALID_REQUEST, ErrorCode.UNAUTHORIZED)
    @GetMapping("/festivals")
    fun festivals(
        @Parameter(description = "꽃 카테고리 필터", example = "CHERRY")
        @RequestParam(name = "category", required = false) category: BloomCategory?,
    ): ResponseEntity<ApiResponse<ExploreFestivalListResponse>>
}
