package com.peakda.server.domain.seasonal.application

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FestivalPlaceTokenizerTest {

    @Test
    fun `venue 의 일반 접미사를 떼고 장소 토큰만 남긴다`() {
        val tokens = FestivalPlaceTokenizer.tokenize(
            venue = "팔공산 갓바위 시설지구 일원",
            festivalName = "2026년 제25회 팔공산 단풍축제",
            addresses = listOf("대구광역시 동구 아양로 207", "대구광역시 동구 신암동 36-1"),
        )

        assertThat(tokens).containsExactlyInAnyOrder("팔공산", "갓바위")
    }

    @Test
    fun `괄호와 물결 구분자를 나눠 토큰으로 만든다`() {
        val tokens = FestivalPlaceTokenizer.tokenize(
            venue = "영산강(극락교~서창교 나눔누리숲)일원",
            festivalName = "광주서창억새축제",
            addresses = listOf("전남광주통합특별시 서구 서창둑길 377"),
        )

        assertThat(tokens).containsExactlyInAnyOrder("영산강", "극락교", "서창교", "나눔누리숲")
    }

    @Test
    fun `생태공원 같은 일반 명사와 행정구역 토큰은 버린다`() {
        val tokens = FestivalPlaceTokenizer.tokenize(
            venue = "강진만 생태공원 일원",
            festivalName = "강진만 춤추는 갈대축제",
            addresses = listOf("전남광주통합특별시 강진군 강진읍 남당로 97-23"),
        )
        val villageTokens = FestivalPlaceTokenizer.tokenize(
            venue = "이산리 식영정 일원",
            festivalName = "2026 몽탄 코스모스 축제",
            addresses = listOf("전남광주통합특별시 무안군 몽탄면 이산리 612"),
        )

        assertThat(tokens).containsExactlyInAnyOrder("강진만")
        assertThat(villageTokens).containsExactlyInAnyOrder("식영정")
    }

    @Test
    fun `하이픈으로 이어진 장소와 일반 시설명을 나눠 처리한다`() {
        val tokens = FestivalPlaceTokenizer.tokenize(
            venue = "경화역-여좌천 일원, 시민공원",
            festivalName = "진해 벚꽃 마라톤",
            addresses = listOf("경상남도 창원시 진해구 충장로 1"),
        )

        assertThat(tokens).containsExactlyInAnyOrder("경화역", "여좌천")
    }

    @Test
    fun `축제명의 지역명은 주소에 포함되면 버린다`() {
        val tokens = FestivalPlaceTokenizer.tokenize(
            venue = "산정호수 일원",
            festivalName = "제29회 포천 산정호수 명성산 억새꽃 축제",
            addresses = listOf("경기도 포천시 영북면 산정호수로 411"),
        )

        assertThat(tokens).containsExactlyInAnyOrder("산정호수", "명성산")
    }
}
