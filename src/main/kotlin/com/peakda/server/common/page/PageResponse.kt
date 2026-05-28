package com.peakda.server.common.page

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Page

@Schema(description = "공통 페이지 응답 (0-based)")
data class PageResponse<T>(
    @field:Schema(description = "현재 페이지의 항목 리스트")
    val content: List<T>,
    @field:Schema(description = "0-based 현재 페이지", example = "0")
    val page: Int,
    @field:Schema(description = "페이지 크기", example = "20")
    val size: Int,
    @field:Schema(description = "전체 항목 수", example = "47")
    val totalElements: Long,
    @field:Schema(description = "전체 페이지 수", example = "3")
    val totalPages: Int,
    @field:Schema(description = "다음 페이지 존재 여부", example = "true")
    val hasNext: Boolean,
) {
    companion object {
        fun <T> of(page: Page<T>): PageResponse<T> = PageResponse(
            content = page.content,
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            hasNext = page.hasNext(),
        )
    }
}

fun <T> Page<T>.toPageResponse(): PageResponse<T> = PageResponse.of(this)
