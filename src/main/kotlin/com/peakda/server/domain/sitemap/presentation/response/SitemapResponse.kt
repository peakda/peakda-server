package com.peakda.server.domain.sitemap.presentation.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "sitemap 용 공개 상세 페이지 목록. 각 목록은 id 오름차순")
data class SitemapResponse(
    @field:Schema(description = "공개 명소 스팟 (동네 스팟 제외)")
    val spots: List<SitemapEntry>,

    @field:Schema(description = "전체 축제")
    val festivals: List<SitemapEntry>,

    @field:Schema(description = "발행된 큐레이션")
    val curations: List<SitemapEntry>,
) {
    @Schema(description = "상세 페이지 id 와 내용의 마지막 수정 시각")
    data class SitemapEntry(
        @field:Schema(description = "상세 id", example = "166")
        val id: Long,

        @field:Schema(description = "상세 화면 내용의 마지막 수정 시각 (UTC)", example = "2026-09-14T00:00:00Z")
        val updatedAt: Instant,
    )
}
