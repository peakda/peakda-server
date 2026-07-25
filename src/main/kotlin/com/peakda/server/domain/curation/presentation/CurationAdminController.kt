package com.peakda.server.domain.curation.presentation

import com.peakda.server.common.response.ApiResponse
import com.peakda.server.domain.curation.application.CurationAdminService
import com.peakda.server.domain.curation.application.toCommand
import com.peakda.server.domain.curation.presentation.request.UpsertCurationRequest
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

    override fun upsert(
        request: UpsertCurationRequest,
    ): ResponseEntity<ApiResponse<CurationIdResponse>> {
        val response = CurationIdResponse(curationAdminService.upsert(request.toCommand()))
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun delete(id: Long): ResponseEntity<ApiResponse<Unit>> {
        curationAdminService.delete(id)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK))
    }
}
