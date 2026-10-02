package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.seasonal.entity.BloomCategory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GalleryFlowerPhotoTest {

    @Test
    fun `제목·키워드의 꽃 이름으로 카테고리를 정하고 촬영 장소에서 시도·시군구를 뗀다`() {
        val photo = GalleryFlowerPhoto.of(
            contentId = "g1",
            title = "마곡사",
            keywords = "마곡사, 충청남도 공주시, 사찰, 단풍, 가을",
            location = "충청남도 공주시 사곡면",
        )!!

        assertThat(photo.categories).containsExactly(BloomCategory.MAPLE)
        assertThat(photo.sido).isEqualTo("충청남도")
        assertThat(photo.sigungu).isEqualTo("공주시")
        assertThat(photo.tokens).contains("마곡사", "충청남도공주시")
    }

    @Test
    fun `꽃 이름이 우연히 들어간 사진은 그 꽃의 근거로 쓰지 않는다`() {
        assertThat(GalleryFlowerPhoto.of("g1", "여수국가산업단지", "여수, 야경", "전남광주통합특별시 여수")).isNull()
        assertThat(GalleryFlowerPhoto.of("g2", "구룡포 근대문화역사거리", "동백꽃 필 무렵, 촬영지", "경상북도 포항시 남구")).isNull()
        assertThat(GalleryFlowerPhoto.of("g3", "한국화 전시", "한국화", "서울특별시 종로구")).isNull()
    }

    @Test
    fun `촬영 장소 뒤에 쉼표로 설명이 붙어도 시군구만 뗀다`() {
        val photo = GalleryFlowerPhoto.of("g1", "우화정의 아침", "단풍", "전라북도 정읍시, 내장산국립공원")!!

        assertThat(photo.sigungu).isEqualTo("정읍시")
        assertThat(GalleryFlowerPhoto.of("g2", "우화정", "단풍", "전라북도 정읍시 내장동, 내장산국립공원")!!.sigungu)
            .isEqualTo("정읍시")
    }

    @Test
    fun `세 글자 이상 명소 이름은 이웃 키워드를 이어 붙인 경우도 찾고 두 글자 이름은 키워드와 같아야 한다`() {
        val photo = GalleryFlowerPhoto.of("g1", "암태도", "진해, 여좌천, 동백, 종묘", "경상남도 창원시")!!

        assertThat(photo.mentions("진해여좌천")).isTrue()
        assertThat(photo.mentions("종묘")).isTrue()
        assertThat(photo.mentions("태도")).isFalse()
    }
}
