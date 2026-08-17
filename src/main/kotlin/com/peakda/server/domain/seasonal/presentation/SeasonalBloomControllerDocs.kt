package com.peakda.server.domain.seasonal.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.seasonal.entity.Region
import com.peakda.server.domain.seasonal.presentation.response.BloomCalendarResponse
import com.peakda.server.domain.seasonal.presentation.response.BloomMapResponse
import com.peakda.server.domain.seasonal.presentation.response.BloomPeakListResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.LocalDate

@Tag(name = "Seasonal Bloom", description = "계절 개화 상태 조회 API")
interface SeasonalBloomControllerDocs {

    @Operation(
        summary = "지도 영역 개화 현황 (Spot 핀)",
        description = "지도 영역(bbox) 내 Spot 핀별 현재 개화 상태를 조회한다. " +
            "명소형(개화 추정 상속)과 동네형(사용자 기록 파생) 핀을 함께 반환하며, " +
            "핀 3단계(PREPARING/STARTED/PEAK)만 노출하고 ENDED 는 제외된다. " +
            "category 와 categories 는 합집합으로 특정 꽃을 필터하고, status 로 명소형·동네형에 같은 상태 기준을 적용한다. " +
            "region 은 bbox 와 AND 로 적용되며 bbox 를 무시하고 권역 전체를 반환하지 않는다. " +
            "동네형은 주소 첫 토큰으로 권역을 판정하고, 판정 불가한 주소는 권역 필터가 걸리면 제외한다. " +
            "date(방문예정일)로 그날 기준 명소형 상태를 재계산할 수 있다. 필터 UI 의 시기 탭은 status 를 쓴다. " +
            "date 는 방문예정일 기반 재계산용이며 동네형은 최근 관측값을 유지하므로 미래 날짜에서는 두 기준이 섞인다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
    )
    @GetMapping
    fun map(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
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
        @Parameter(
            description = "꽃 카테고리 반복 필터. category 와 함께 지정하면 합집합으로 처리한다.",
            example = "CHERRY",
        )
        @RequestParam("categories", required = false) categories: List<BloomCategory>?,
        @Parameter(description = "개화 상태 필터. 명소형 추정과 동네형 최근 관측에 동일하게 적용한다.", example = "PEAK")
        @RequestParam("status", required = false) status: BloomStatus?,
        @Parameter(
            description = "단일 권역 필터. bbox 와 AND 로 적용한다. 동네형은 주소 첫 토큰으로 판정하며, 판정 불가 시 제외한다.",
            example = "CAPITAL",
        )
        @RequestParam("region", required = false) region: Region?,
        @Parameter(
            description = "방문예정일 (생략 시 오늘 기준). 명소형 핀 상태를 해당일 기준으로 재계산한다. 동네형은 최근 관측값 유지. " +
                "필터 UI 의 시기 탭은 status 를 쓴다. date 는 방문예정일 기반 재계산용이며, 동네형은 최근 관측값을 유지하므로 미래 날짜에서는 두 기준이 섞인다.",
            example = "2026-04-01",
        )
        @RequestParam("date", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        date: LocalDate?,
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
