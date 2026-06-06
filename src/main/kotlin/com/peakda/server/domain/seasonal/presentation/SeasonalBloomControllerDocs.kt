package com.peakda.server.domain.seasonal.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.presentation.response.BloomCalendarResponse
import com.peakda.server.domain.seasonal.presentation.response.BloomMapResponse
import com.peakda.server.domain.seasonal.presentation.response.BloomPeakListResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Seasonal Bloom", description = "계절 개화 상태 조회 API")
interface SeasonalBloomControllerDocs {

    @Operation(
        summary = "지도 영역 개화 현황",
        description = "지도 영역(bbox) 내 visible 명소별 현재 개화 상태를 조회한다. " +
            "핀 3단계(PREPARING/STARTED/PEAK)만 노출하며 ENDED 는 제외된다. category 로 특정 꽃만 필터할 수 있다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
    )
    @GetMapping
    fun map(
        @Parameter(description = "남서 위도", example = "37.4")
        @RequestParam("minLat") minLat: Double,
        @Parameter(description = "북동 위도", example = "37.7")
        @RequestParam("maxLat") maxLat: Double,
        @Parameter(description = "남서 경도", example = "126.8")
        @RequestParam("minLng") minLng: Double,
        @Parameter(description = "북동 경도", example = "127.1")
        @RequestParam("maxLng") maxLng: Double,
        @Parameter(description = "꽃 카테고리 필터 (생략 시 전체)", example = "CHERRY")
        @RequestParam("category", required = false) category: BloomCategory?,
    ): ResponseEntity<ApiResponse<BloomMapResponse>>

    @Operation(
        summary = "지금이 절정인 명소 리스트",
        description = "최신 산출일 기준 status=PEAK 명소를 조회한다. category 로 특정 꽃만 필터할 수 있다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
    )
    @GetMapping("/peak")
    fun peak(
        @Parameter(description = "꽃 카테고리 필터 (생략 시 전체)", example = "CHERRY")
        @RequestParam("category", required = false) category: BloomCategory?,
    ): ResponseEntity<ApiResponse<BloomPeakListResponse>>

    @Operation(
        summary = "예상 만개 캘린더",
        description = "단일 명소×카테고리의 향후 일별 예상 상태 타임라인과 대표 절정 구간(올해 만개 시기/지속일)을 온디맨드로 계산한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.ATTRACTION_NOT_FOUND,
    )
    @GetMapping("/calendar")
    fun calendar(
        @Parameter(description = "명소 id", example = "501")
        @RequestParam("attractionId") attractionId: Long,
        @Parameter(description = "꽃 카테고리", example = "CHERRY")
        @RequestParam("category") category: BloomCategory,
    ): ResponseEntity<ApiResponse<BloomCalendarResponse>>
}
