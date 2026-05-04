package com.peakda.server.infrastructure.external.kma.midfcst

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MidRegionCodeTest {
    @Test
    fun `region code exposes land temperature and sea codes separately`() {
        assertThat(MidRegionCode.SEOUL.landRegId).isEqualTo("11B00000")
        assertThat(MidRegionCode.SEOUL.temperatureRegId).isEqualTo("11B10101")
        assertThat(MidRegionCode.BUSAN.landRegId).isEqualTo("11H20000")
        assertThat(MidRegionCode.BUSAN.temperatureRegId).isEqualTo("11H20201")
        assertThat(MidRegionCode.BUSAN.seaRegId).isEqualTo("12C10000")
    }

    @Test
    fun `display name lookup returns region code`() {
        assertThat(MidRegionCode.fromDisplayName("제주")).isEqualTo(MidRegionCode.JEJU)
        assertThat(MidRegionCode.fromDisplayName("없는 지역")).isNull()
    }
}
