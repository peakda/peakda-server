package com.peakda.server.domain.festival.presentation

import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.page.toPageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.festival.application.FestivalEditorialAdminService
import com.peakda.server.domain.festival.application.toCommand
import com.peakda.server.domain.festival.presentation.request.UpsertFestivalEditorialRequest
import com.peakda.server.domain.festival.presentation.response.FestivalAdminSummaryResponse
import com.peakda.server.domain.festival.presentation.response.FestivalEditorialAdminResponse
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

    override fun list(
        query: String?,
        pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<FestivalAdminSummaryResponse>>> {
        val response = festivalEditorialAdminService
            .list(query, pageRequest.toPageable())
            .toPageResponse()
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun editorial(festivalId: Long): ResponseEntity<ApiResponse<FestivalEditorialAdminResponse>> {
        val response = festivalEditorialAdminService.editorial(festivalId)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun upsert(
        principal: PrincipalDetails,
        festivalId: Long,
        request: UpsertFestivalEditorialRequest,
    ): ResponseEntity<ApiResponse<FestivalEditorialIdResponse>> {
        val adminId = requireNotNull(principal.getUser().id)
        val editorialId = festivalEditorialAdminService.upsert(adminId, festivalId, request.toCommand())
        return ResponseEntity.ok(
            ApiResponse.success(HttpStatus.OK, FestivalEditorialIdResponse(editorialId)),
        )
    }

    override fun delete(principal: PrincipalDetails, festivalId: Long): ResponseEntity<ApiResponse<Unit>> {
        val adminId = requireNotNull(principal.getUser().id)
        festivalEditorialAdminService.delete(adminId, festivalId)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK))
    }
}
