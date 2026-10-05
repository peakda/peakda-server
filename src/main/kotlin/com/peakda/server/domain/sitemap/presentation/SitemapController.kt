package com.peakda.server.domain.sitemap.presentation

import com.peakda.server.common.response.ApiResponse
import com.peakda.server.domain.sitemap.application.SitemapService
import com.peakda.server.domain.sitemap.presentation.response.SitemapResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/sitemap")
class SitemapController(
    private val sitemapService: SitemapService,
) : SitemapControllerDocs {

    override fun sitemap(): ResponseEntity<ApiResponse<SitemapResponse>> =
        ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, sitemapService.sitemap()))
}
