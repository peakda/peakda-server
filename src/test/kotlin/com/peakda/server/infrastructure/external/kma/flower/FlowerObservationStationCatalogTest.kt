package com.peakda.server.infrastructure.external.kma.flower

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.ClassPathResource

class FlowerObservationStationCatalogTest {
    @Test
    fun `배포되는 매핑 파일은 벚나무 관측 13행을 모두 담고 있다`() {
        val catalog = FlowerObservationStationCatalog(ClassPathResource(DEFAULT_RESOURCE_PATH))

        assertThat(catalog.stationByPlace).hasSize(13)
        assertThat(catalog.stationByPlace.values).contains("108", "112", "289")
    }

    @Test
    fun `주석과 헤더 행은 건너뛴다`() {
        val csv = """
            # 주석
            obsPlace,stnId,stnName
            여의도 윤중로,108,서울
        """.trimIndent()

        val catalog = FlowerObservationStationCatalog(ByteArrayResource(csv.toByteArray()))

        assertThat(catalog.stationByPlace).containsExactlyEntriesOf(mapOf("여의도 윤중로" to "108"))
    }

    @Test
    fun `장소명으로 지점번호를 찾는다`() {
        val catalog = FlowerObservationStationCatalog(ClassPathResource(DEFAULT_RESOURCE_PATH))

        assertThat(catalog.stationByPlace["여의도 윤중로"]).isEqualTo("108")
        assertThat(catalog.stationByPlace["경주 보문관광단지"]).isEqualTo("283")
    }

    @Test
    fun `지점 행이 없으면 기동 시점에 실패한다`() {
        val csv = "# 주석\nobsPlace,stnId,stnName"

        assertThatThrownBy { FlowerObservationStationCatalog(ByteArrayResource(csv.toByteArray())) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("비어 있습니다")
    }

    companion object {
        private const val DEFAULT_RESOURCE_PATH = "external/kma/flower-observation-stations.csv"
    }
}
