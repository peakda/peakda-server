package com.peakda.server.domain.congestion.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.congestion.entity.CongestionLinkStatus
import com.peakda.server.domain.congestion.presentation.request.ReviewCongestionLinkRequest
import com.peakda.server.domain.congestion.presentation.response.CongestionLinkAdminResponse
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

@Tag(name = "Congestion Link Admin", description = "관광지 집중률 ↔ 명소 연결 검토 관리자 API")
interface CongestionLinkAdminControllerDocs {

    @Operation(
        summary = "혼잡도 연결 목록 조회",
        description = "매칭 잡이 만든 연결을 상태별로 조회한다. 기본값은 검토 대기(PENDING_REVIEW)이며 후보 명소를 함께 내려준다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
    )
    @GetMapping
    fun list(
        @Parameter(description = "연결 상태", example = "PENDING_REVIEW")
        @RequestParam(name = "status", defaultValue = "PENDING_REVIEW") status: CongestionLinkStatus,
        @Valid @ModelAttribute pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<CongestionLinkAdminResponse>>>

    @Operation(
        summary = "혼잡도 연결 확정/거절",
        description = "CONFIRM 은 지정한 명소(없으면 제안된 명소)로 확정하고, REJECT 는 연결하지 않기로 결정한다. " +
            "확정·거절한 연결은 매칭 잡이 다시 바꾸지 않는다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
        ErrorCode.CONGESTION_LINK_NOT_FOUND,
        ErrorCode.CONGESTION_LINK_ATTRACTION_REQUIRED,
        ErrorCode.ATTRACTION_NOT_FOUND,
    )
    @PatchMapping("/{id}")
    fun review(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Parameter(description = "연결 id", example = "42")
        @PathVariable("id") id: Long,
        @Valid @RequestBody request: ReviewCongestionLinkRequest,
    ): ResponseEntity<ApiResponse<CongestionLinkAdminResponse>>
}
