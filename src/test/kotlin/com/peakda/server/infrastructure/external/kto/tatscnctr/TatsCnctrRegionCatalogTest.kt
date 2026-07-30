package com.peakda.server.infrastructure.external.kto.tatscnctr

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.ClassPathResource

class TatsCnctrRegionCatalogTest {
    @Test
    fun `배포되는 코드파일은 전국 시군구를 모두 담고 있다`() {
        val catalog = TatsCnctrRegionCatalog(ClassPathResource(DEFAULT_RESOURCE_PATH))

        assertThat(catalog.all).hasSize(252)
        assertThat(catalog.all.map { it.areaCd }.distinct()).hasSize(17)
        assertThat(catalog.all).contains(
            TatsCnctrRegion(areaCd = "11", signguCd = "11110"),
            TatsCnctrRegion(areaCd = "51", signguCd = "51130"),
        )
        assertThat(catalog.all).doesNotHaveDuplicates()
        assertThat(catalog.all).allSatisfy {
            assertThat(it.areaCd).matches("\\d{2}")
            assertThat(it.signguCd).matches("\\d{5}")
        }
    }

    @Test
    fun `주석과 헤더 행은 건너뛴다`() {
        val csv = """
            # 주석
            areaCd,areaNm,sigunguCd,sigunguNm
            11,서울특별시,11110,종로구
        """.trimIndent()

        val catalog = TatsCnctrRegionCatalog(ByteArrayResource(csv.toByteArray()))

        assertThat(catalog.all).containsExactly(TatsCnctrRegion(areaCd = "11", signguCd = "11110"))
    }

    @Test
    fun `코드 행이 없으면 기동 시점에 실패한다`() {
        val csv = "areaCd,areaNm,sigunguCd,sigunguNm"

        assertThatThrownBy { TatsCnctrRegionCatalog(ByteArrayResource(csv.toByteArray())) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("비어 있습니다")
    }

    @Test
    fun `컬럼이 모자라면 실패한다`() {
        val csv = "11,서울특별시"

        assertThatThrownBy { TatsCnctrRegionCatalog(ByteArrayResource(csv.toByteArray())) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("올바르지 않습니다")
    }

    companion object {
        private const val DEFAULT_RESOURCE_PATH = "external/kto/tats-cnctr-sigungu-codes.csv"
    }
}
