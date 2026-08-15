package com.peakda.server.domain.search.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.search.presentation.response.SpotSearchItem
import com.peakda.server.domain.search.presentation.response.TrendingSpotsResponse
import com.peakda.server.domain.search.presentation.response.UserSearchItem
import com.peakda.server.domain.seasonal.entity.BloomCategory
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Search", description = "검색 API")
interface SearchControllerDocs {

    @Operation(
        summary = "스팟 검색",
        description = "스팟명 부분일치(대소문자 무시)로 검색한다. 비공개(visible=false) 스팟은 제외된다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
    )
    @GetMapping("/spots")
    fun searchSpots(
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Parameter(description = "검색어", example = "남산")
        @RequestParam("q") query: String,
        @Parameter(description = "개화 신호가 있는 꽃 카테고리로 결과를 제한한다", example = "CHERRY")
        @RequestParam("category", required = false) category: BloomCategory?,
        @Valid @ModelAttribute pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<SpotSearchItem>>>

    @Operation(
        summary = "사용자 검색",
        description = "닉네임 부분일치(대소문자 무시)로 검색한다. 탈퇴(익명화)한 사용자는 제외된다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
    )
    @GetMapping("/users")
    fun searchUsers(
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Parameter(description = "검색어 (닉네임)", example = "피크다")
        @RequestParam("q") query: String,
        @Valid @ModelAttribute pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<UserSearchItem>>>

    @Operation(
        summary = "인기 검색 (트렌딩 스팟)",
        description = "찜이 많은 순 상위 스팟 목록. 최근 검색어는 서버에 저장하지 않으므로(결정 H) " +
            "찜 수를 대체 인기 신호로 쓴다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.UNAUTHORIZED,
    )
    @GetMapping("/trending")
    fun trending(): ResponseEntity<ApiResponse<TrendingSpotsResponse>>
}
