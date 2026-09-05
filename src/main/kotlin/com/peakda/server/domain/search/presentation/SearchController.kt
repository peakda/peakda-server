package com.peakda.server.domain.search.presentation

import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.domain.search.application.SearchService
import com.peakda.server.domain.search.presentation.response.SpotSearchItem
import com.peakda.server.domain.search.presentation.response.TrendingSpotsResponse
import com.peakda.server.domain.search.presentation.response.UserSearchItem
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.seasonal.entity.BloomCategory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/search")
class SearchController(
    private val searchService: SearchService,
) : SearchControllerDocs {

    override fun searchSpots(
        principal: PrincipalDetails,
        query: String,
        category: BloomCategory?,
        pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<SpotSearchItem>>> {
        val response = searchService.searchSpots(requireNotNull(principal.getUser().id), query, pageRequest, category)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun searchUsers(
        principal: PrincipalDetails,
        query: String,
        pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<UserSearchItem>>> {
        val response = searchService.searchUsers(requireNotNull(principal.getUser().id), query, pageRequest)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun trending(): ResponseEntity<ApiResponse<TrendingSpotsResponse>> {
        val response = searchService.trending()
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }
}
