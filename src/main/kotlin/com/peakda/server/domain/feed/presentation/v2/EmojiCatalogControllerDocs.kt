package com.peakda.server.domain.feed.presentation.v2

import com.peakda.server.common.response.ApiResponse
import com.peakda.server.domain.feed.presentation.v2.response.EmojiCatalogResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.context.request.WebRequest

@Tag(name = "Feed V2")
interface EmojiCatalogControllerDocs {
    @Operation(summary = "지원하는 전체 기본 이모지 카탈로그", description = "피커에서 제공할 키·이모지·그룹과 Unicode 데이터 버전을 반환한다. ETag와 If-None-Match를 지원한다.")
    @GetMapping
    fun get(@Parameter(hidden = true) request: WebRequest): ResponseEntity<ApiResponse<EmojiCatalogResponse>>
}
