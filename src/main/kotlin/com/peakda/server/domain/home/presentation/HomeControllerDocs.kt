package com.peakda.server.domain.home.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.domain.home.presentation.response.HomeSuggestionResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping

@Tag(name = "Home", description = "홈 화면 보조 API")
interface HomeControllerDocs {

    @Operation(
        summary = "시즌 추천어",
        description = "홈 검색바 보조 카피 1건. 최신 산출일 기준 신뢰도가 가장 높은 절정(PEAK) 명소×카테고리로 " +
            "\"요즘 절정인 {꽃}, {명소}에서 만나보세요\" 카피를 만든다. 절정 데이터가 없으면 available=false.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.UNAUTHORIZED,
    )
    @GetMapping("/suggestion")
    fun suggestion(): ResponseEntity<ApiResponse<HomeSuggestionResponse>>
}
