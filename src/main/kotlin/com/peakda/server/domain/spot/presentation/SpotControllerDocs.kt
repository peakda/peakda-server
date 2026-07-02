package com.peakda.server.domain.spot.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.spot.presentation.request.SpotMatchRequest
import com.peakda.server.domain.spot.presentation.response.SpotDetailResponse
import com.peakda.server.domain.spot.presentation.response.SpotMatchResponse
import com.peakda.server.domain.spot.presentation.response.SpotPreviewResponse
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
import org.springframework.web.bind.annotation.RequestParam

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

    @Operation(
        summary = "핀 클릭 프리뷰",
        description = "지도 핀 탭 시 보여줄 카드(썸네일/개화 단계 뱃지/거리)를 조회한다. " +
            "spotIds 1건이면 단일 프리뷰(SCR-011e), 여러 건이면 클러스터 리스트(SCR-011d)로 그대로 쓸 수 있다. " +
            "lat/lng 을 함께 주면 각 스팟까지의 거리(m)를 계산해 채운다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
    )
    @GetMapping("/preview")
    fun preview(
        @Parameter(description = "프리뷰할 스팟 id 목록", example = "100,101")
        @RequestParam("spotIds") spotIds: List<Long>,
        @Parameter(description = "꽃 카테고리 필터 (생략 시 각 스팟의 대표 단계)", example = "CHERRY")
        @RequestParam("category", required = false) category: BloomCategory?,
        @Parameter(description = "거리 계산 기준 위도 (lng 과 함께 생략 가능)", example = "37.55")
        @RequestParam("lat", required = false) lat: Double?,
        @Parameter(description = "거리 계산 기준 경도 (lat 과 함께 생략 가능)", example = "126.98")
        @RequestParam("lng", required = false) lng: Double?,
    ): ResponseEntity<ApiResponse<SpotPreviewResponse>>
}
