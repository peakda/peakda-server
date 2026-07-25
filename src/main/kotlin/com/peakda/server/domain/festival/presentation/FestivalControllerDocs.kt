package com.peakda.server.domain.festival.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.domain.festival.presentation.response.FestivalDetailResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@Tag(name = "Festival", description = "축제 API")
interface FestivalControllerDocs {

    @Operation(
        summary = "축제 상세",
        description = "발행된 에디토리얼이 없으면 `editorial = null`이고 축제 기본 정보만 내려간다. " +
            "상태 뱃지·기간 일수는 정규화된 시작·종료일로 서버가 계산한다. " +
            "표시 문자열은 프론트가 포맷한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(ErrorCode.UNAUTHORIZED, ErrorCode.FESTIVAL_NOT_FOUND)
    @GetMapping("/{id}")
    fun detail(
        @Parameter(description = "축제 id", example = "101")
        @PathVariable("id") id: Long,
    ): ResponseEntity<ApiResponse<FestivalDetailResponse>>
}
