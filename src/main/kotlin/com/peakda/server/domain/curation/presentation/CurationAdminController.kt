package com.peakda.server.domain.curation.presentation

import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.page.toPageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.curation.application.CurationAdminService
import com.peakda.server.domain.curation.application.toCommand
import com.peakda.server.domain.curation.entity.CurationStatus
import com.peakda.server.domain.curation.presentation.request.UpsertCurationRequest
import com.peakda.server.domain.curation.presentation.response.CurationAdminDetailResponse
import com.peakda.server.domain.curation.presentation.response.CurationAdminSummaryResponse
import com.peakda.server.domain.curation.presentation.response.CurationIdResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/curations")
class CurationAdminController(
    private val curationAdminService: CurationAdminService,
) : CurationAdminControllerDocs {

    override fun list(
        status: CurationStatus?,
        pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<CurationAdminSummaryResponse>>> {
        val response = curationAdminService
            .list(status, pageRequest.toPageable())
            .toPageResponse()
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun detail(id: Long): ResponseEntity<ApiResponse<CurationAdminDetailResponse>> {
        val response = curationAdminService.detail(id)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun upsert(
        principal: PrincipalDetails,
        request: UpsertCurationRequest,
    ): ResponseEntity<ApiResponse<CurationIdResponse>> {
        val adminId = requireNotNull(principal.getUser().id)
        val response = CurationIdResponse(curationAdminService.upsert(adminId, request.toCommand()))
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun delete(principal: PrincipalDetails, id: Long): ResponseEntity<ApiResponse<Unit>> {
        val adminId = requireNotNull(principal.getUser().id)
        curationAdminService.delete(adminId, id)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK))
    }
}
