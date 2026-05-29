package com.peakda.server.domain.spot.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.spot.presentation.request.UpdateFavoriteNotifyRequest
import com.peakda.server.domain.spot.presentation.response.SpotFavoriteListResponse
import com.peakda.server.domain.spot.presentation.response.SpotFavoriteResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "Spot Favorite", description = "스팟 찜 API")
interface SpotFavoriteControllerDocs {

    @Operation(
        summary = "스팟 찜 추가",
        description = "스팟을 찜한다. 이미 찜한 스팟이면 기존 찜을 그대로 반환한다 (멱등). " +
            "찜 시 만개 알림이 기본 활성화된다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.UNAUTHORIZED,
        ErrorCode.SPOT_NOT_FOUND,
    )
    @PostMapping("/{spotId}")
    fun add(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @PathVariable("spotId") spotId: Long,
    ): ResponseEntity<ApiResponse<SpotFavoriteResponse>>

    @Operation(
        summary = "스팟 찜 취소",
        description = "찜을 해제한다. 찜하지 않은 스팟이어도 성공으로 응답한다 (멱등).",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(ErrorCode.UNAUTHORIZED)
    @DeleteMapping("/{spotId}")
    fun remove(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @PathVariable("spotId") spotId: Long,
    ): ResponseEntity<ApiResponse<Unit>>

    @Operation(
        summary = "찜한 스팟 만개 알림 설정 변경",
        description = "찜한 스팟의 만개 알림 수신 여부를 변경한다. 찜하지 않은 스팟이면 404.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.SPOT_FAVORITE_NOT_FOUND,
    )
    @PatchMapping("/{spotId}/notify")
    fun updateNotify(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @PathVariable("spotId") spotId: Long,
        @Valid @RequestBody request: UpdateFavoriteNotifyRequest,
    ): ResponseEntity<ApiResponse<SpotFavoriteResponse>>

    @Operation(
        summary = "찜한 스팟 목록",
        description = "본인이 찜한 스팟을 최근 찜한 순으로 조회한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(ErrorCode.UNAUTHORIZED)
    @GetMapping
    fun list(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
    ): ResponseEntity<ApiResponse<SpotFavoriteListResponse>>
}
