package com.peakda.server.domain.weather.application

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MidForecastRegionResolverTest {
    @Test
    fun `시도 코드로 중기예보 구역을 정한다`() {
        assertThat(MidForecastRegionResolver.resolve("11", "11710")).isEqualTo("SEOUL")
        assertThat(MidForecastRegionResolver.resolve("48", "48129")).isEqualTo("GYEONGNAM")
        assertThat(MidForecastRegionResolver.resolve("50", "50110")).isEqualTo("JEJU")
    }

    @Test
    fun `강원은 영동 시군구만 영동 구역이고 나머지는 영서 구역이다`() {
        assertThat(MidForecastRegionResolver.resolve("51", "51150")).isEqualTo("GANGWON_YEONGDONG")
        assertThat(MidForecastRegionResolver.resolve("51", "51110")).isEqualTo("GANGWON_YEONGSEO")
    }

    @Test
    fun `전남광주통합특별시는 옛 광주 자치구만 광주 구역이고 나머지는 전남 구역이다`() {
        assertThat(MidForecastRegionResolver.resolve("12", "12210")).isEqualTo("GWANGJU")
        assertThat(MidForecastRegionResolver.resolve("12", "12870")).isEqualTo("JEONNAM")
    }

    @Test
    fun `법정동 코드가 없으면 구역을 정하지 않는다`() {
        assertThat(MidForecastRegionResolver.resolve(null, null)).isNull()
        assertThat(MidForecastRegionResolver.resolve("99", "99110")).isNull()
    }
}
