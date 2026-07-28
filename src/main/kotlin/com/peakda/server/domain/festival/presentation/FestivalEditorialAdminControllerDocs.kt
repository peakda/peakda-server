package com.peakda.server.domain.festival.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.domain.festival.presentation.request.UpsertFestivalEditorialRequest
import com.peakda.server.domain.festival.presentation.response.FestivalEditorialIdResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "Festival Admin", description = "축제 에디토리얼 관리자 API")
interface FestivalEditorialAdminControllerDocs {

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
        @Parameter(description = "축제 id", example = "101")
        @PathVariable("festivalId") festivalId: Long,
    ): ResponseEntity<ApiResponse<Unit>>
}
