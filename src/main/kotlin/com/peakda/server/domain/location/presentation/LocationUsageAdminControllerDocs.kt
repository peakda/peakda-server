package com.peakda.server.domain.location.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.domain.location.entity.LocationServiceType
import com.peakda.server.domain.location.presentation.response.LocationUsageLogResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestParam
import java.time.Instant

@Tag(name = "Admin Location Usage", description = "백오피스 위치정보 이용·제공사실 확인자료 API")
interface LocationUsageAdminControllerDocs {

    @Operation(
        summary = "위치정보 이용·제공사실 확인자료 목록 조회",
        description = "개인위치정보를 이용한 요청 기록을 최신순으로 페이징 조회한다. " +
            "대상 이메일 검색어, 제공서비스, 이용일시 구간으로 좁힐 수 있다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
    )
    @GetMapping
    fun list(
        @Parameter(description = "대상 사용자 이메일 검색어 (부분일치)", example = "ex1@xxx.com")
        @RequestParam(name = "email", required = false) email: String?,
        @Parameter(description = "제공서비스 필터 (생략 시 전체)", example = "BLOOM_MAP")
        @RequestParam(name = "service", required = false) service: LocationServiceType?,
        @Parameter(description = "이용일시 시작 (이 시각 이후)", example = "2026-08-01T00:00:00Z")
        @RequestParam(name = "from", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        from: Instant?,
        @Parameter(description = "이용일시 종료 (이 시각 이전)", example = "2026-08-31T23:59:59Z")
        @RequestParam(name = "to", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        to: Instant?,
        @Valid @ModelAttribute pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<LocationUsageLogResponse>>>
}
