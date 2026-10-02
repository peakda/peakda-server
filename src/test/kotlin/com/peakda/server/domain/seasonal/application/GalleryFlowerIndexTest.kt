package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.attraction.entity.Attraction
import com.peakda.server.domain.seasonal.entity.BloomCategory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GalleryFlowerIndexTest {

    @Test
    fun `같은 시군구의 꽃 사진에 명소 이름이 나오면 그 꽃의 근거로 연결한다`() {
        val index = GalleryFlowerIndex(
            listOf(photo("g1", "백양사추경의흐름", "백양사, 사찰, 단풍", "전남광주통합특별시 장성군 북하면")),
        )

        assertThat(index.match(attraction("백양사(장성)", "전남광주통합특별시 장성군 북하면 백양로 1239")))
            .containsExactlyEntriesOf(mapOf(BloomCategory.MAPLE to "g1"))
        assertThat(index.match(attraction("백양사", "충청남도 다른군 어딘가"))).isEmpty()
    }

    @Test
    fun `광역시 구는 시 이름까지 맞아야 연결한다`() {
        val index = GalleryFlowerIndex(listOf(photo("g1", "대구 안일사", "안일사, 단풍", "대구광역시 남구 대명동")))

        assertThat(index.match(attraction("안일사", "대구광역시 남구 앞산순환로"))).containsKey(BloomCategory.MAPLE)
        assertThat(index.match(attraction("안일사", "부산광역시 남구 어딘가"))).isEmpty()
    }

    @Test
    fun `여러 사진이 근거면 id 가 가장 작은 사진을 남기고 꽃마다 따로 연결한다`() {
        val index = GalleryFlowerIndex(
            listOf(
                photo("g2", "녹산로", "녹산로 유채꽃도로, 벚꽃", "제주특별자치도 서귀포시 표선면"),
                photo("g1", "녹산로 유채꽃도로", "유채, 벚꽃", "제주특별자치도 서귀포시 표선면"),
            ),
        )

        val evidence = index.match(attraction("녹산로 유채꽃도로", "제주특별자치도 서귀포시 표선면 가시로565번길 20"))

        assertThat(evidence).containsExactlyInAnyOrderEntriesOf(
            mapOf(BloomCategory.CANOLA to "g1", BloomCategory.CHERRY to "g1"),
        )
    }

    private fun photo(id: String, title: String, keywords: String, location: String) =
        GalleryFlowerPhoto.of(id, title, keywords, location)!!

    private fun attraction(title: String, address: String) =
        Attraction(tourApiContentId = "content", contentTypeCode = "12", title = title, addressMain = address)
}
