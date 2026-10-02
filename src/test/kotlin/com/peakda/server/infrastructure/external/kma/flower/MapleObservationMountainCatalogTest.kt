package com.peakda.server.infrastructure.external.kma.flower

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.ClassPathResource

class MapleObservationMountainCatalogTest {
    @Test
    fun `배포되는 목록은 기상청 유명산 단풍 관측 21개 산을 모두 담고 있다`() {
        val catalog = MapleObservationMountainCatalog(ClassPathResource(DEFAULT_RESOURCE_PATH))

        assertThat(catalog.areaCodesByMountain).hasSize(21)
        assertThat(catalog.areaCodesByMountain["설악산"]).containsExactly("51")
        assertThat(catalog.areaCodesByMountain["지리산"]).containsExactlyInAnyOrder("12", "52", "48")
    }

    @Test
    fun `주석과 헤더를 건너뛰고 시도 코드를 세미콜론으로 나눈다`() {
        val csv = """
            # 주석
            obsPlace,areaCodes
            북한산, 11 ; 41
        """.trimIndent()

        val catalog = MapleObservationMountainCatalog(ByteArrayResource(csv.toByteArray()))

        assertThat(catalog.areaCodesByMountain).containsExactlyEntriesOf(mapOf("북한산" to setOf("11", "41")))
    }

    @Test
    fun `시도 코드가 없는 행이 있으면 기동 시점에 실패한다`() {
        val csv = "obsPlace,areaCodes\n설악산,"

        assertThatThrownBy { MapleObservationMountainCatalog(ByteArrayResource(csv.toByteArray())) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("형식이 올바르지 않습니다")
    }

    companion object {
        private const val DEFAULT_RESOURCE_PATH = "external/kma/maple-observation-mountains.csv"
    }
}
