package com.peakda.server.domain.spot.presentation

import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.page.toPageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.spot.application.PlantAdminService
import com.peakda.server.domain.spot.entity.PlantStatus
import com.peakda.server.domain.spot.presentation.request.UpdatePlantRequest
import com.peakda.server.domain.spot.presentation.response.PlantAdminResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/plants")
class PlantAdminController(
    private val plantAdminService: PlantAdminService,
) : PlantAdminControllerDocs {

    override fun list(
        status: PlantStatus?,
        suggestedOnly: Boolean,
        pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<PlantAdminResponse>>> {
        val response = plantAdminService
            .list(status, suggestedOnly, pageRequest.toPageable())
            .toPageResponse()
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun update(
        principal: PrincipalDetails,
        id: Long,
        request: UpdatePlantRequest,
    ): ResponseEntity<ApiResponse<PlantAdminResponse>> {
        val adminId = requireNotNull(principal.getUser().id)
        val response = plantAdminService.update(adminId, id, request.toCommand())
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }
}
