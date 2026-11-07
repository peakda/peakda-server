package com.peakda.server.domain.feed.presentation.v2

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.feed.entity.FeedFilter
import com.peakda.server.domain.feed.presentation.v2.response.FeedRecordResponse
import com.peakda.server.domain.feed.presentation.v2.response.FeedRecordSummaryResponse
import com.peakda.server.domain.feed.presentation.v2.response.ReactionPageResponse
import com.peakda.server.domain.feed.presentation.v2.response.ReactionStateResponse
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
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Feed V2", description = "전체 기본 이모지를 지원하는 피드 API")
interface FeedV2ControllerDocs {
    @Operation(summary = "v2 피드 목록", description = "공개 기록을 최신순으로 조회한다. 비로그인은 ALL만 허용한다. 반응은 최대 8종 미리보기다.")
    @ApiErrorResponses(ErrorCode.INVALID_REQUEST, ErrorCode.UNAUTHORIZED)
    @GetMapping
    fun list(
        @Parameter(hidden = true) @AuthenticationPrincipal principal: PrincipalDetails?,
        @Parameter(description = "피드 필터", example = "ALL")
        @RequestParam("filter", defaultValue = "ALL") filter: FeedFilter,
        @Valid @ModelAttribute pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<FeedRecordSummaryResponse>>>

    @Operation(summary = "v2 피드 상세", description = "게시된 기록과 최대 8종의 반응 미리보기를 조회한다. DRAFT·미존재는 404다.")
    @ApiErrorResponses(ErrorCode.SPOT_RECORD_NOT_FOUND)
    @GetMapping("/{id}")
    fun get(
        @Parameter(hidden = true) @AuthenticationPrincipal principal: PrincipalDetails?,
        @Parameter(description = "기록 ID", example = "1024") @PathVariable("id") id: Long,
    ): ResponseEntity<ApiResponse<FeedRecordResponse>>

    @Operation(summary = "이모지 반응 추가", description = "카탈로그의 키로 추가한다. 동일 반응 재요청은 성공하며 알림을 중복 발행하지 않는다.", security = [SecurityRequirement(name = "accessTokenCookie")])
    @ApiErrorResponses(ErrorCode.INVALID_REQUEST, ErrorCode.UNAUTHORIZED, ErrorCode.SPOT_RECORD_NOT_FOUND)
    @PutMapping("/{id}/reactions/{emojiKey}")
    fun addReaction(
        @Parameter(hidden = true) @AuthenticationPrincipal principal: PrincipalDetails,
        @Parameter(description = "기록 ID", example = "1024") @PathVariable("id") id: Long,
        @Parameter(description = "카탈로그의 정규 이모지 키", example = "u:1f44d") @PathVariable("emojiKey") emojiKey: String,
    ): ResponseEntity<ApiResponse<ReactionStateResponse>>

    @Operation(summary = "이모지 반응 취소", description = "현재 사용자의 반응만 삭제한다. 이미 없으면 성공한다.", security = [SecurityRequirement(name = "accessTokenCookie")])
    @ApiErrorResponses(ErrorCode.INVALID_REQUEST, ErrorCode.UNAUTHORIZED, ErrorCode.SPOT_RECORD_NOT_FOUND)
    @DeleteMapping("/{id}/reactions/{emojiKey}")
    fun removeReaction(
        @Parameter(hidden = true) @AuthenticationPrincipal principal: PrincipalDetails,
        @Parameter(description = "기록 ID", example = "1024") @PathVariable("id") id: Long,
        @Parameter(description = "카탈로그의 정규 이모지 키", example = "u:1f44d") @PathVariable("emojiKey") emojiKey: String,
    ): ResponseEntity<ApiResponse<ReactionStateResponse>>

    @Operation(summary = "전체 반응 집계 조회", description = "사용된 이모지를 키 오름차순으로 조회한다. nextAfterKey로 다음 페이지를 요청한다. 비로그인 mineOnly는 빈 목록이다.")
    @ApiErrorResponses(ErrorCode.INVALID_REQUEST, ErrorCode.SPOT_RECORD_NOT_FOUND)
    @GetMapping("/{id}/reactions")
    fun reactions(
        @Parameter(hidden = true) @AuthenticationPrincipal principal: PrincipalDetails?,
        @Parameter(description = "기록 ID", example = "1024") @PathVariable("id") id: Long,
        @Parameter(description = "이전 응답의 nextAfterKey", example = "u:1f44d") @RequestParam("afterKey", required = false) afterKey: String?,
        @Parameter(description = "페이지 크기 (1~100)", example = "50") @RequestParam("size", defaultValue = "50") size: Int,
        @Parameter(description = "본인이 남긴 반응만 조회", example = "false") @RequestParam("mineOnly", defaultValue = "false") mineOnly: Boolean,
    ): ResponseEntity<ApiResponse<ReactionPageResponse>>
}
