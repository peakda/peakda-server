package com.peakda.server.domain.spot.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.spot.presentation.request.SpotMatchRequest
import com.peakda.server.domain.spot.presentation.response.SpotDetailResponse
import com.peakda.server.domain.spot.presentation.response.SpotMatchResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "Spot", description = "스팟 (지도/검색 단위) API")
interface SpotControllerDocs {

    @Operation(
        summary = "좌표 기반 스팟 매칭",
        description = "카카오 검색에서 받은 좌표/이름/(선택) placeId 로 기존 스팟을 매칭한다. " +
            "kakaoPlaceId 가 일치하는 LOCAL 스팟이 있으면 그것이 우선, " +
            "없으면 반경 내 가장 가까운 ATTRACTION 을 찾아 매칭한다. " +
            "ATTRACTION 매칭 시 spots 행이 없으면 생성하여 id 를 부여한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
    )
    @PostMapping("/match")
    fun match(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Valid @RequestBody request: SpotMatchRequest,
    ): ResponseEntity<ApiResponse<SpotMatchResponse>>

    @Operation(
        summary = "스팟 상세 조회",
        description = "스팟 단위 상세 화면 정보를 반환한다. 대표 사진, 올해 만개 시기 배너(개화 추정 연동), " +
            "게시된 방문 기록 수와 최신 프리뷰, 현재 사용자의 찜 상태를 포함한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.UNAUTHORIZED,
        ErrorCode.SPOT_NOT_FOUND,
    )
    @GetMapping("/{id}")
    fun getSpotDetail(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Parameter(description = "스팟 id", example = "100")
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<SpotDetailResponse>>
}
