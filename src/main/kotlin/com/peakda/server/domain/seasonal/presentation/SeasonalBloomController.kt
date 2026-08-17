package com.peakda.server.domain.seasonal.presentation

import com.peakda.server.common.response.ApiResponse
import com.peakda.server.domain.location.application.RecordLocationUsage
import com.peakda.server.domain.location.entity.LocationServiceType
import com.peakda.server.domain.seasonal.application.BloomCalendarService
import com.peakda.server.domain.seasonal.application.BloomQueryService
import com.peakda.server.domain.seasonal.application.SpotBloomMapService
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.seasonal.entity.Region
import com.peakda.server.domain.seasonal.presentation.response.BloomCalendarResponse
import com.peakda.server.domain.seasonal.presentation.response.BloomMapResponse
import com.peakda.server.domain.seasonal.presentation.response.BloomPeakListResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/seasonal/blooms")
class SeasonalBloomController(
    private val bloomQueryService: BloomQueryService,
    private val bloomCalendarService: BloomCalendarService,
    private val spotBloomMapService: SpotBloomMapService,
) : SeasonalBloomControllerDocs {

    @RecordLocationUsage(service = LocationServiceType.BLOOM_MAP)
    override fun map(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double,
        category: BloomCategory?,
        categories: List<BloomCategory>?,
        status: BloomStatus?,
        region: Region?,
        date: LocalDate?,
    ): ResponseEntity<ApiResponse<BloomMapResponse>> {
        val selectedCategories = (categories.orEmpty() + listOfNotNull(category)).distinct().ifEmpty { null }
        val response = spotBloomMapService.map(
            minLat = minLat,
            maxLat = maxLat,
            minLng = minLng,
            maxLng = maxLng,
            categories = selectedCategories,
            status = status,
            region = region,
            date = date,
        )
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun peak(
        category: BloomCategory?,
    ): ResponseEntity<ApiResponse<BloomPeakListResponse>> {
        val response = bloomQueryService.peakList(category)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun calendar(
        attractionId: Long,
        category: BloomCategory,
    ): ResponseEntity<ApiResponse<BloomCalendarResponse>> {
        val response = bloomCalendarService.getCalendar(attractionId, category)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }
}
