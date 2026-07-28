package com.peakda.server.domain.festival.presentation

import com.peakda.server.common.response.ApiResponse
import com.peakda.server.domain.festival.application.FestivalEditorialAdminService
import com.peakda.server.domain.festival.application.toCommand
import com.peakda.server.domain.festival.presentation.request.UpsertFestivalEditorialRequest
import com.peakda.server.domain.festival.presentation.response.FestivalEditorialIdResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/festivals")
class FestivalEditorialAdminController(
    private val festivalEditorialAdminService: FestivalEditorialAdminService,
) : FestivalEditorialAdminControllerDocs {

    override fun upsert(
        festivalId: Long,
        request: UpsertFestivalEditorialRequest,
    ): ResponseEntity<ApiResponse<FestivalEditorialIdResponse>> {
        val editorialId = festivalEditorialAdminService.upsert(festivalId, request.toCommand())
        return ResponseEntity.ok(
            ApiResponse.success(HttpStatus.OK, FestivalEditorialIdResponse(editorialId)),
        )
    }

    override fun delete(festivalId: Long): ResponseEntity<ApiResponse<Unit>> {
        festivalEditorialAdminService.delete(festivalId)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK))
    }
}
