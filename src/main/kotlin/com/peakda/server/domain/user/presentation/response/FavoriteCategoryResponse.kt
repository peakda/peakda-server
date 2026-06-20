package com.peakda.server.domain.user.presentation.response

import com.peakda.server.domain.seasonal.entity.BloomCategory
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "사용자의 관심 꽃 카테고리 목록")
data class FavoriteCategoryResponse(
    @field:Schema(description = "관심 꽃 카테고리")
    val categories: List<Item>,
) {
    @Schema(description = "관심 꽃 카테고리 1건")
    data class Item(
        @field:Schema(description = "카테고리", example = "CHERRY")
        val category: BloomCategory,
        @field:Schema(description = "카테고리 표시명", example = "벚꽃")
        val displayName: String,
    )

    companion object {
        fun of(categories: Collection<BloomCategory>): FavoriteCategoryResponse =
            FavoriteCategoryResponse(
                categories.sortedBy { it.ordinal }
                    .map { Item(it, it.displayName) },
            )
    }
}
