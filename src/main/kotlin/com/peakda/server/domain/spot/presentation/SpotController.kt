package com.peakda.server.domain.spot.presentation

import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.spot.application.SpotDetailService
import com.peakda.server.domain.spot.application.SpotMatcher
import com.peakda.server.domain.spot.application.SpotService
import com.peakda.server.domain.spot.entity.Spot
import com.peakda.server.domain.spot.entity.SpotType
import com.peakda.server.domain.spot.presentation.request.SpotMatchRequest
import com.peakda.server.domain.spot.presentation.response.SpotDetailResponse
import com.peakda.server.domain.spot.presentation.response.SpotMatchResponse
import com.peakda.server.domain.spot.presentation.response.SpotMatchResponse.MatchedSpot
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/spots")
class SpotController(
    private val spotMatcher: SpotMatcher,
    private val spotService: SpotService,
    private val spotDetailService: SpotDetailService,
) : SpotControllerDocs {

    override fun match(
        principal: PrincipalDetails,
        request: SpotMatchRequest,
    ): ResponseEntity<ApiResponse<SpotMatchResponse>> {
        val result = spotMatcher.match(request.latitude, request.longitude, request.kakaoPlaceId)
        val response = when (result) {
            is SpotMatcher.MatchResult.ExistingSpot ->
                SpotMatchResponse(matched = true, spot = result.spot.toMatched(), suggestedType = result.spot.type)
            is SpotMatcher.MatchResult.NearbyAttraction -> {
                val spot = spotService.findOrCreateForAttraction(result.attraction)
                SpotMatchResponse(matched = true, spot = spot.toMatched(), suggestedType = SpotType.ATTRACTION)
            }
            SpotMatcher.MatchResult.NoMatch ->
                SpotMatchResponse(matched = false, spot = null, suggestedType = SpotType.LOCAL)
        }
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun getSpotDetail(
        principal: PrincipalDetails,
        id: Long,
    ): ResponseEntity<ApiResponse<SpotDetailResponse>> {
        val userId = requireNotNull(principal.getUser().id)
        val response = spotDetailService.getDetail(id, userId)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    private fun Spot.toMatched(): MatchedSpot = MatchedSpot(
        id = requireNotNull(id),
        type = type,
        name = name,
        address = address,
        attractionId = attractionId,
    )
}
