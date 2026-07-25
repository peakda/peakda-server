package com.peakda.server.domain.explore.presentation

import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.explore.application.ExploreSection
import com.peakda.server.domain.explore.application.ExploreService
import com.peakda.server.domain.explore.presentation.response.ExploreFestivalListResponse
import com.peakda.server.domain.explore.presentation.response.ExploreResponse
import com.peakda.server.domain.explore.presentation.response.ExploreResponse.ExploreSpotItem
import com.peakda.server.domain.seasonal.entity.BloomCategory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.ZoneId

@RestController
@RequestMapping("/api/explore")
class ExploreController(
    private val exploreService: ExploreService,
) : ExploreControllerDocs {

    override fun explore(
        principal: PrincipalDetails,
        category: BloomCategory?,
    ): ResponseEntity<ApiResponse<ExploreResponse>> {
        val userId = requireNotNull(principal.getUser().id)
        val response = exploreService.explore(userId, category, LocalDate.now(KST))
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun spots(
        principal: PrincipalDetails,
        section: ExploreSection,
        category: BloomCategory?,
        pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<ExploreSpotItem>>> {
        val userId = requireNotNull(principal.getUser().id)
        val response = exploreService.spots(userId, section, category, pageRequest)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun festivals(
        category: BloomCategory?,
    ): ResponseEntity<ApiResponse<ExploreFestivalListResponse>> {
        val response = exploreService.festivals(category, LocalDate.now(KST))
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    companion object {
        private val KST = ZoneId.of("Asia/Seoul")
    }
}
