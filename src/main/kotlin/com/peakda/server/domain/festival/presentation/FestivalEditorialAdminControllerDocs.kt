package com.peakda.server.domain.festival.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.festival.presentation.request.UpsertFestivalEditorialRequest
import com.peakda.server.domain.festival.presentation.response.FestivalAdminSummaryResponse
import com.peakda.server.domain.festival.presentation.response.FestivalEditorialAdminResponse
import com.peakda.server.domain.festival.presentation.response.FestivalEditorialIdResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Festival Admin", description = "축제 에디토리얼 관리자 API")
interface FestivalEditorialAdminControllerDocs {

    @Operation(
        summary = "관리자 축제 목록 조회",
        description = "축제명 부분 검색(q)은 대소문자를 구분하지 않는다. q가 없으면 전체 축제를 최신 id 순으로 조회한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
    )
    @GetMapping
    fun list(
        @Parameter(description = "축제명 부분 검색어", example = "해바라기")
        @RequestParam(name = "q", required = false) query: String?,
        @Valid @ModelAttribute pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<FestivalAdminSummaryResponse>>>

    @Operation(
        summary = "관리자 축제 에디토리얼 조회",
        description = "기존 편집 데이터를 원본 object key와 미리보기 URL로 내려준다. 에디토리얼이 없으면 404이다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
        ErrorCode.FESTIVAL_EDITORIAL_NOT_FOUND,
    )
    @GetMapping("/{festivalId}/editorial")
    fun editorial(
        @Parameter(description = "축제 id", example = "101")
        @PathVariable("festivalId") festivalId: Long,
    ): ResponseEntity<ApiResponse<FestivalEditorialAdminResponse>>

    @Operation(
        summary = "축제 에디토리얼 등록·수정",
        description = "축제 id 기준으로 멱등 등록·수정하고 주요 볼거리를 요청 배열 전체로 교체한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
        ErrorCode.FESTIVAL_NOT_FOUND,
    )
    @PutMapping("/{festivalId}/editorial")
    fun upsert(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Parameter(description = "축제 id", example = "101")
        @PathVariable("festivalId") festivalId: Long,
        @Valid @RequestBody request: UpsertFestivalEditorialRequest,
    ): ResponseEntity<ApiResponse<FestivalEditorialIdResponse>>

    @Operation(
        summary = "축제 에디토리얼 삭제",
        description = "주요 볼거리를 먼저 지운 뒤 축제 에디토리얼을 삭제한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
        ErrorCode.FESTIVAL_EDITORIAL_NOT_FOUND,
    )
    @DeleteMapping("/{festivalId}/editorial")
    fun delete(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Parameter(description = "축제 id", example = "101")
        @PathVariable("festivalId") festivalId: Long,
    ): ResponseEntity<ApiResponse<Unit>>
}
