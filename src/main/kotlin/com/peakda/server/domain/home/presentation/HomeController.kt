package com.peakda.server.domain.home.presentation

import com.peakda.server.common.response.ApiResponse
import com.peakda.server.domain.home.application.HomeSuggestionService
import com.peakda.server.domain.home.presentation.response.HomeSuggestionResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/home")
class HomeController(
    private val homeSuggestionService: HomeSuggestionService,
) : HomeControllerDocs {

    override fun suggestion(): ResponseEntity<ApiResponse<HomeSuggestionResponse>> {
        val response = homeSuggestionService.suggestion()
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }
}
