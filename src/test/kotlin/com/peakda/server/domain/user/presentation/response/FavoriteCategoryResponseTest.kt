package com.peakda.server.domain.user.presentation.response

import com.peakda.server.domain.seasonal.entity.BloomCategory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FavoriteCategoryResponseTest {

    @Test
    fun `카테고리를 ordinal 순으로 정렬하고 표시명을 매핑한다`() {
        val response = FavoriteCategoryResponse.of(setOf(BloomCategory.MAPLE, BloomCategory.CHERRY))

        assertThat(response.categories).extracting("category")
            .containsExactly(BloomCategory.CHERRY, BloomCategory.MAPLE)
        assertThat(response.categories.first().displayName).isEqualTo("벚꽃")
    }

    @Test
    fun `빈 입력이면 빈 목록을 반환한다`() {
        val response = FavoriteCategoryResponse.of(emptySet())

        assertThat(response.categories).isEmpty()
    }
}
