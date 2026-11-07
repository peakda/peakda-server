package com.peakda.server.domain.feed.presentation.v2

import com.peakda.server.common.response.ApiResponse
import com.peakda.server.domain.feed.application.EmojiCatalog
import com.peakda.server.domain.feed.presentation.v2.response.EmojiCatalogResponse
import com.peakda.server.domain.feed.presentation.v2.response.EmojiDefinitionResponse
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.WebRequest
import java.time.Duration

@RestController
@RequestMapping("/api/v2/reaction-emojis")
class EmojiCatalogController(private val catalog: EmojiCatalog) : EmojiCatalogControllerDocs {
    private val response = EmojiCatalogResponse(catalog.version, catalog.items.map { EmojiDefinitionResponse(it.key, it.emoji, it.group) })
    private val cacheControl = CacheControl.maxAge(Duration.ofDays(1)).cachePublic()

    override fun get(request: WebRequest): ResponseEntity<ApiResponse<EmojiCatalogResponse>> {
        if (request.checkNotModified(catalog.etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(catalog.etag).cacheControl(cacheControl).build()
        }
        return ResponseEntity.ok().eTag(catalog.etag).cacheControl(cacheControl)
            .body(ApiResponse.success(HttpStatus.OK, response))
    }
}
