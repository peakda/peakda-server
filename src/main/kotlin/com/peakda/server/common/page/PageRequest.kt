package com.peakda.server.common.page

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.domain.PageRequest as SpringPageRequest

@Schema(description = "공통 페이지 요청 (0-based)")
data class PageRequest(
    @field:Schema(description = "0-based 페이지 번호", example = "0", defaultValue = "0")
    @field:Min(0)
    val page: Int = 0,

    @field:Schema(description = "페이지 크기 (최대 50)", example = "20", defaultValue = "20")
    @field:Min(1)
    @field:Max(50)
    val size: Int = 20,
) {
    fun toPageable(sort: Sort = Sort.unsorted()): Pageable =
        SpringPageRequest.of(page, size, sort)
}
