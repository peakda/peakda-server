package com.peakda.server.domain.congestion.presentation

import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.page.toPageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.congestion.application.CongestionLinkAdminService
import com.peakda.server.domain.congestion.entity.CongestionLinkStatus
import com.peakda.server.domain.congestion.presentation.request.ReviewCongestionLinkRequest
import com.peakda.server.domain.congestion.presentation.response.CongestionLinkAdminResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/congestion-links")
class CongestionLinkAdminController(
    private val congestionLinkAdminService: CongestionLinkAdminService,
) : CongestionLinkAdminControllerDocs {

    override fun list(
        status: CongestionLinkStatus,
        pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<CongestionLinkAdminResponse>>> {
        val response = congestionLinkAdminService.list(status, pageRequest.toPageable()).toPageResponse()
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun review(
        principal: PrincipalDetails,
        id: Long,
        request: ReviewCongestionLinkRequest,
    ): ResponseEntity<ApiResponse<CongestionLinkAdminResponse>> {
        val adminId = requireNotNull(principal.getUser().id)
        val response = congestionLinkAdminService.review(adminId, id, request.toCommand())
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }
}
