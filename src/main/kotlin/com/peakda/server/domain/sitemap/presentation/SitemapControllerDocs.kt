package com.peakda.server.domain.sitemap.presentation

import com.peakda.server.common.response.ApiResponse
import com.peakda.server.domain.sitemap.presentation.response.SitemapResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping

@Tag(name = "Sitemap", description = "검색엔진 sitemap 용 공개 페이지 목록 API")
interface SitemapControllerDocs {

    @Operation(
        summary = "sitemap 용 공개 상세 페이지 목록",
        description = "비로그인으로 조회할 수 있다. 웹 `/sitemap.xml` 생성용이며 하루 한 번 호출을 가정한다. " +
            "스팟은 공개 명소 스팟만 담고 동네(LOCAL) 스팟은 뺀다. 축제는 전체, 큐레이션은 발행된 것만 담는다. " +
            "`updatedAt` 은 상세 화면 내용의 마지막 수정 시각으로 sitemap `lastmod` 에 쓴다. " +
            "스팟은 게시된 방문 기록, 축제는 발행된 에디토리얼의 수정까지 반영하며 매일 다시 산출되는 개화 추정은 반영하지 않는다.",
    )
    @GetMapping
    fun sitemap(): ResponseEntity<ApiResponse<SitemapResponse>>
}
