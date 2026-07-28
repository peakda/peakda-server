package com.peakda.server.domain.spot.presentation

import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.spot.application.SpotRecordAdminService
import com.peakda.server.domain.spot.presentation.request.UpdateSpotRecordStatusRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/spot-records")
class SpotRecordAdminController(
    private val spotRecordAdminService: SpotRecordAdminService,
) : SpotRecordAdminControllerDocs {

    override fun updateStatus(
        principal: PrincipalDetails,
        id: Long,
        request: UpdateSpotRecordStatusRequest,
    ): ResponseEntity<ApiResponse<Unit>> {
        val adminId = requireNotNull(principal.getUser().id)
        spotRecordAdminService.updateStatus(adminId, id, request.status)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK))
    }
}
