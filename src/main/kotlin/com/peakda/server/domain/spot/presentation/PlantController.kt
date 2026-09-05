package com.peakda.server.domain.spot.presentation

import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.spot.application.PlantService
import com.peakda.server.domain.spot.application.SuggestPlantCommand
import com.peakda.server.domain.spot.presentation.request.SuggestPlantRequest
import com.peakda.server.domain.spot.presentation.response.PlantResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/plants")
class PlantController(
    private val plantService: PlantService,
) : PlantControllerDocs {

    override fun list(
        principal: PrincipalDetails,
    ): ResponseEntity<ApiResponse<List<PlantResponse>>> {
        val response = plantService.listActive()
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun search(
        principal: PrincipalDetails,
        keyword: String,
    ): ResponseEntity<ApiResponse<List<PlantResponse>>> {
        val response = plantService.search(keyword)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun suggest(
        principal: PrincipalDetails,
        request: SuggestPlantRequest,
    ): ResponseEntity<ApiResponse<PlantResponse>> {
        val userId = requireNotNull(principal.getUser().id)
        val response = plantService.suggest(SuggestPlantCommand(userId = userId, name = request.name))
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.CREATED, response))
    }
}
