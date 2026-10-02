package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.attraction.entity.Attraction
import com.peakda.server.infrastructure.external.kma.flower.MapleObservationMountainCatalog
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.io.ByteArrayResource

class MapleMountainMatcherTest {
    private val matcher = MapleMountainMatcher(
        MapleObservationMountainCatalog(
            ByteArrayResource(
                """
                    obsPlace,areaCodes
                    설악산,51
                    가야산,48;47
                    속리산,43;47
                """.trimIndent().toByteArray(),
            ),
        ),
    )

    @Test
    fun `제목에 산 이름이 있고 산이 걸친 시도의 명소면 그 산에 연결한다`() {
        assertThat(matcher.mountainOf(attraction("설악산 국립공원(외설악)", areaCode = "51"))).isEqualTo("설악산")
        assertThat(matcher.mountainOf(attraction("가야산 해인사", areaCode = "48"))).isEqualTo("가야산")
    }

    @Test
    fun `이름이 같아도 산이 걸치지 않은 시도의 명소는 연결하지 않는다`() {
        assertThat(matcher.mountainOf(attraction("서산 가야산", areaCode = "44"))).isNull()
    }

    @Test
    fun `다른 꽃 이름이 들어간 명소는 그 꽃의 명소로 보고 연결하지 않는다`() {
        assertThat(matcher.mountainOf(attraction("속리산연꽃단지", areaCode = "43"))).isNull()
    }

    @Test
    fun `법정동 시도 코드가 없으면 연결하지 않는다`() {
        assertThat(matcher.mountainOf(attraction("설악산 국립공원(외설악)", areaCode = null))).isNull()
    }

    private fun attraction(title: String, areaCode: String?) = Attraction(
        tourApiContentId = "content",
        contentTypeCode = "12",
        title = title,
        legalDongAreaCode = areaCode,
    )
}
