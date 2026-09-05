package com.peakda.server.domain.location.presentation

import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.page.toPageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.domain.location.application.LocationUsageQueryService
import com.peakda.server.domain.location.entity.LocationServiceType
import com.peakda.server.domain.location.presentation.response.LocationUsageLogResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/admin/location-usage-logs")
class LocationUsageAdminController(
    private val locationUsageQueryService: LocationUsageQueryService,
) : LocationUsageAdminControllerDocs {

    override fun list(
        email: String?,
        service: LocationServiceType?,
        from: Instant?,
        to: Instant?,
        pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<LocationUsageLogResponse>>> {
        val response = locationUsageQueryService
            .list(email, service, from, to, pageRequest.toPageable())
            .toPageResponse()
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }
}
