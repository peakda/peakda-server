package com.peakda.server.domain.seasonal.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BloomCategoryTest {

    @Test
    fun `해바라기와 국화 축제는 각각 새 카테고리로 태깅된다`() {
        assertThat(BloomCategory.ofFestivalName("태백 해바라기축제")).isEqualTo(BloomCategory.SUNFLOWER)
        assertThat(BloomCategory.ofFestivalName("구리 국화축제")).isEqualTo(BloomCategory.CHRYSANTHEMUM)
    }

    @Test
    fun `새 카테고리는 온도 주도 모델 대상이 아니다`() {
        assertThat(BloomCategory.SUNFLOWER.temperatureDriven).isFalse()
        assertThat(BloomCategory.CHRYSANTHEMUM.temperatureDriven).isFalse()
    }

    @Test
    fun `핑크뮬리를 유지하면서 카테고리는 15종이다`() {
        assertThat(BloomCategory.entries).hasSize(15)
        assertThat(BloomCategory.entries).contains(BloomCategory.PINK_MUHLY)
    }
}
