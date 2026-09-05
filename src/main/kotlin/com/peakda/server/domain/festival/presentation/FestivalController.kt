package com.peakda.server.domain.festival.presentation

import com.peakda.server.common.response.ApiResponse
import com.peakda.server.domain.festival.application.FestivalDetailService
import com.peakda.server.domain.festival.presentation.response.FestivalDetailResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.ZoneId

@RestController
@RequestMapping("/api/festivals")
class FestivalController(
    private val festivalDetailService: FestivalDetailService,
) : FestivalControllerDocs {

    override fun detail(id: Long): ResponseEntity<ApiResponse<FestivalDetailResponse>> {
        val response = festivalDetailService.detail(id, LocalDate.now(KST))
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    companion object {
        private val KST = ZoneId.of("Asia/Seoul")
    }
}
