package com.peakda.server.domain.spot.presentation

import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.spot.application.SpotFavoriteService
import com.peakda.server.domain.spot.presentation.request.UpdateFavoriteNotifyRequest
import com.peakda.server.domain.spot.presentation.response.SpotFavoriteListResponse
import com.peakda.server.domain.spot.presentation.response.SpotFavoriteResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/spots/favorites")
class SpotFavoriteController(
    private val spotFavoriteService: SpotFavoriteService,
) : SpotFavoriteControllerDocs {

    override fun add(
        principal: PrincipalDetails,
        spotId: Long,
    ): ResponseEntity<ApiResponse<SpotFavoriteResponse>> {
        val userId = requireNotNull(principal.getUser().id)
        val response = spotFavoriteService.add(userId, spotId)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun remove(
        principal: PrincipalDetails,
        spotId: Long,
    ): ResponseEntity<ApiResponse<Unit>> {
        val userId = requireNotNull(principal.getUser().id)
        spotFavoriteService.remove(userId, spotId)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK))
    }

    override fun updateNotify(
        principal: PrincipalDetails,
        spotId: Long,
        request: UpdateFavoriteNotifyRequest,
    ): ResponseEntity<ApiResponse<SpotFavoriteResponse>> {
        val userId = requireNotNull(principal.getUser().id)
        val response = spotFavoriteService.updateNotify(userId, spotId, request.enabled)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun list(
        principal: PrincipalDetails,
    ): ResponseEntity<ApiResponse<SpotFavoriteListResponse>> {
        val userId = requireNotNull(principal.getUser().id)
        val response = spotFavoriteService.list(userId)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }
}
