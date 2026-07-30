package com.peakda.server.infrastructure.external.kma.asosdaly

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.ClassPathResource

class AsosStationCatalogTest {
    @Test
    fun `배포되는 지점파일은 ASOS 96개 지점을 모두 담고 있다`() {
        val catalog = AsosStationCatalog(ClassPathResource(DEFAULT_RESOURCE_PATH))

        assertThat(catalog.all).hasSize(96)
        assertThat(catalog.all.map { it.stnId }).doesNotHaveDuplicates()
    }

    @Test
    fun `주석과 헤더 행은 건너뛴다`() {
        val csv = """
            # 주석
            stnId,name,latitude,longitude,altitude
            108,서울,37.57142,126.9658,85.67
        """.trimIndent()

        val catalog = AsosStationCatalog(ByteArrayResource(csv.toByteArray()))

        assertThat(catalog.all).containsExactly(
            AsosStation("108", "서울", 37.57142, 126.9658, 85.67),
        )
    }

    @Test
    fun `서울 지점의 번호 좌표 고도를 파싱한다`() {
        val catalog = AsosStationCatalog(ClassPathResource(DEFAULT_RESOURCE_PATH))

        assertThat(catalog.all).contains(
            AsosStation("108", "서울", 37.57142, 126.9658, 85.67),
        )
    }

    @Test
    fun `지점 행이 없으면 기동 시점에 실패한다`() {
        val csv = "# 주석\nstnId,name,latitude,longitude,altitude"

        assertThatThrownBy { AsosStationCatalog(ByteArrayResource(csv.toByteArray())) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("비어 있습니다")
    }

    companion object {
        private const val DEFAULT_RESOURCE_PATH = "external/kma/asos-stations.csv"
    }
}
