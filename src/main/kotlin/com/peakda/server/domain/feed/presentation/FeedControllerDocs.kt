package com.peakda.server.domain.feed.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.feed.entity.FeedFilter
import com.peakda.server.domain.feed.presentation.response.FeedReactionSummaryResponse
import com.peakda.server.domain.spot.entity.ReactionType
import com.peakda.server.domain.spot.presentation.response.SpotRecordResponse
import com.peakda.server.domain.spot.presentation.response.SpotRecordSummaryResponse
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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Feed", description = "피드 API")
interface FeedControllerDocs {

    @Operation(
        summary = "피드 조회",
        description = "게시된 스팟 기록을 filter 로 필터링해 최신순으로 조회한다. " +
            "all=전체, interest=관심 꽃(사용자 관심 카테고리와 겹치는 식물이 태깅된 기록), following=팔로잉 중인 사용자의 기록.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
    )
    @GetMapping
    fun list(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Parameter(description = "피드 필터", example = "ALL")
        @RequestParam("filter") filter: FeedFilter,
        @Valid @ModelAttribute pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<SpotRecordSummaryResponse>>>

    @Operation(
        summary = "피드 상세 조회",
        description = "게시된(PUBLISHED) 기록만 조회 가능. DRAFT 이거나 존재하지 않으면 404.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.UNAUTHORIZED,
        ErrorCode.SPOT_RECORD_NOT_FOUND,
    )
    @GetMapping("/{id}")
    fun get(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Parameter(description = "스팟 기록 id", example = "1024")
        @PathVariable("id") id: Long,
    ): ResponseEntity<ApiResponse<SpotRecordResponse>>

    @Operation(
        summary = "리액션 추가",
        description = "게시된 기록에 리액션을 추가한다 (결정 F). 이미 남긴 리액션이면 그대로 반환한다 (멱등). " +
            "DRAFT 이거나 존재하지 않으면 404.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.UNAUTHORIZED,
        ErrorCode.SPOT_RECORD_NOT_FOUND,
    )
    @PostMapping("/{id}/reactions")
    fun addReaction(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Parameter(description = "스팟 기록 id", example = "1024")
        @PathVariable("id") id: Long,
        @Parameter(description = "리액션 타입", example = "HEART")
        @RequestParam("reactionType") reactionType: ReactionType,
    ): ResponseEntity<ApiResponse<FeedReactionSummaryResponse>>

    @Operation(
        summary = "리액션 취소",
        description = "남긴 리액션을 취소한다. 남기지 않은 리액션이어도 성공으로 응답한다 (멱등).",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.UNAUTHORIZED,
        ErrorCode.SPOT_RECORD_NOT_FOUND,
    )
    @DeleteMapping("/{id}/reactions")
    fun removeReaction(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Parameter(description = "스팟 기록 id", example = "1024")
        @PathVariable("id") id: Long,
        @Parameter(description = "리액션 타입", example = "HEART")
        @RequestParam("reactionType") reactionType: ReactionType,
    ): ResponseEntity<ApiResponse<FeedReactionSummaryResponse>>
}
