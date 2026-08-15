package com.peakda.server.domain.seasonal.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RegionTest {

    @Test
    fun `Tour API 경계 코드 세종과 울산을 각각 충청과 경상으로 매핑한다`() {
        assertThat(Region.ofAreaCode("8")).isEqualTo(Region.CHUNGCHEONG)
        assertThat(Region.ofAreaCode("7")).isEqualTo(Region.GYEONGSANG)
    }

    @Test
    fun `LOCAL 주소는 첫 토큰으로 권역을 판정하고 알 수 없는 주소는 제외한다`() {
        assertThat(Region.ofAddress("세종특별자치시 조치원읍")).isEqualTo(Region.CHUNGCHEONG)
        assertThat(Region.ofAddress("울산광역시 남구")).isEqualTo(Region.GYEONGSANG)
        assertThat(Region.ofAddress("알 수 없는 주소")).isNull()
        assertThat(Region.ofAddress(null)).isNull()
    }
}
