package com.peakda.server.domain.festival.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.festival.presentation.request.SaveFestivalRequest
import com.peakda.server.domain.festival.presentation.response.FestivalAdminDetailResponse
import com.peakda.server.domain.festival.presentation.response.FestivalIdResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "Festival Admin", description = "축제·트리 명소와 에디토리얼 관리자 API")
interface FestivalAdminControllerDocs {

    @Operation(
        summary = "관리자 축제 기본 정보 조회",
        description = "공공데이터·직접 등록 축제의 기본 정보와 오늘 기준 단계를 조회한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
        ErrorCode.FESTIVAL_NOT_FOUND,
    )
    @GetMapping("/{festivalId}")
    fun detail(
        @Parameter(description = "축제 id", example = "9001")
        @PathVariable("festivalId") festivalId: Long,
    ): ResponseEntity<ApiResponse<FestivalAdminDetailResponse>>

    @Operation(
        summary = "축제·트리 명소 직접 등록",
        description = "공공데이터에 없는 축제나 트리 명소를 등록한다. 직접 등록한 행은 공공데이터 동기화가 덮어쓰지 않는다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
        ErrorCode.FESTIVAL_INVALID_PERIOD,
        ErrorCode.FESTIVAL_INVALID_COORDINATES,
        ErrorCode.FESTIVAL_DUPLICATED,
    )
    @PostMapping
    fun create(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Valid @RequestBody request: SaveFestivalRequest,
    ): ResponseEntity<ApiResponse<FestivalIdResponse>>

    @Operation(
        summary = "직접 등록 축제·트리 명소 수정",
        description = "직접 등록한 축제의 기본 정보를 요청 값으로 전부 교체한다. 공공데이터 축제는 수정할 수 없다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
        ErrorCode.FESTIVAL_NOT_FOUND,
        ErrorCode.FESTIVAL_NOT_EDITABLE,
        ErrorCode.FESTIVAL_INVALID_PERIOD,
        ErrorCode.FESTIVAL_INVALID_COORDINATES,
        ErrorCode.FESTIVAL_DUPLICATED,
    )
    @PutMapping("/{festivalId}")
    fun update(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Parameter(description = "축제 id", example = "9001")
        @PathVariable("festivalId") festivalId: Long,
        @Valid @RequestBody request: SaveFestivalRequest,
    ): ResponseEntity<ApiResponse<Unit>>

    @Operation(
        summary = "직접 등록 축제·트리 명소 삭제",
        description = "직접 등록한 축제를 에디토리얼·주요 볼거리와 함께 삭제한다. 공공데이터 축제는 삭제할 수 없다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
        ErrorCode.FESTIVAL_NOT_FOUND,
        ErrorCode.FESTIVAL_NOT_EDITABLE,
    )
    @DeleteMapping("/{festivalId}")
    fun delete(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Parameter(description = "축제 id", example = "9001")
        @PathVariable("festivalId") festivalId: Long,
    ): ResponseEntity<ApiResponse<Unit>>
}
