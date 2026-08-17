package com.peakda.server.domain.curation.presentation

import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.page.toPageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.domain.curation.application.CurationQueryService
import com.peakda.server.domain.curation.presentation.response.CurationCardResponse
import com.peakda.server.domain.location.application.RecordLocationUsage
import com.peakda.server.domain.location.entity.LocationServiceType
import com.peakda.server.domain.curation.presentation.response.CurationDetailResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/curations")
class CurationController(
    private val curationQueryService: CurationQueryService,
) : CurationControllerDocs {

    override fun list(
        pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<CurationCardResponse>>> {
        val response = curationQueryService.cards(pageRequest.toPageable()).toPageResponse()
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    @RecordLocationUsage(service = LocationServiceType.CURATION_DETAIL, coordinateParams = ["lat", "lng"])
    override fun detail(
        id: Long,
        lat: Double?,
        lng: Double?,
    ): ResponseEntity<ApiResponse<CurationDetailResponse>> {
        val response = curationQueryService.detail(id, lat, lng)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }
}
