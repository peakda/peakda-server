package com.peakda.server.domain.curation.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.curation.entity.CurationStatus
import com.peakda.server.domain.curation.presentation.request.UpsertCurationRequest
import com.peakda.server.domain.curation.presentation.response.CurationAdminDetailResponse
import com.peakda.server.domain.curation.presentation.response.CurationAdminSummaryResponse
import com.peakda.server.domain.curation.presentation.response.CurationIdResponse
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

@Tag(name = "Curation Admin", description = "큐레이션 관리자 API")
interface CurationAdminControllerDocs {

    @Operation(
        summary = "큐레이션 관리자 목록 조회",
        description = "상태 필터가 없으면 모든 상태를 최신 주차순으로 조회한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
    )
    @GetMapping
    fun list(
        @Parameter(description = "큐레이션 상태. 생략하면 전체", example = "DRAFT")
        @RequestParam(name = "status", required = false) status: CurationStatus?,
        @Valid @ModelAttribute pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<CurationAdminSummaryResponse>>>

    @Operation(
        summary = "큐레이션 관리자 상세 조회",
        description = "DRAFT/PUBLISHED 상태와 관계없이 저장된 챕터·추천 값을 정렬 순서대로 조회한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
        ErrorCode.CURATION_NOT_FOUND,
    )
    @GetMapping("/{id}")
    fun detail(
        @Parameter(description = "큐레이션 id", example = "101")
        @PathVariable("id") id: Long,
    ): ResponseEntity<ApiResponse<CurationAdminDetailResponse>>

    @Operation(
        summary = "큐레이션 등록·수정",
        description = "weekStartDate 기준으로 멱등 등록·수정하고 챕터·추천을 요청 배열 전체로 교체한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
        ErrorCode.CURATION_NOT_FOUND,
    )
    @PutMapping
    fun upsert(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Valid @RequestBody request: UpsertCurationRequest,
    ): ResponseEntity<ApiResponse<CurationIdResponse>>

    @Operation(
        summary = "큐레이션 삭제",
        description = "큐레이션의 챕터·추천을 먼저 지운 뒤 큐레이션을 삭제한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
        ErrorCode.CURATION_NOT_FOUND,
    )
    @DeleteMapping("/{id}")
    fun delete(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Parameter(description = "큐레이션 id", example = "101")
        @PathVariable("id") id: Long,
    ): ResponseEntity<ApiResponse<Unit>>
}
