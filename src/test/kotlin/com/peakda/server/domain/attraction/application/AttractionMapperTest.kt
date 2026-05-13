package com.peakda.server.domain.attraction.application

import com.peakda.server.infrastructure.external.kto.korservice.response.AreaBasedSyncListItem
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AttractionMapperTest {

    @Test
    fun `AreaBasedSyncListItem 을 Attraction 으로 매핑한다`() {
        val item = AreaBasedSyncListItem(
            addr1 = "서울시 종로구",
            contentid = "126128",
            contenttypeid = "12",
            title = "경복궁",
            mapx = "126.977",
            mapy = "37.578",
            modifiedtime = "20260501120000",
            showflag = "1",
            cat1 = "A02",
        )

        val attraction = item.toAttraction()

        assertThat(attraction.contentId).isEqualTo("126128")
        assertThat(attraction.title).isEqualTo("경복궁")
        assertThat(attraction.mapX).isEqualTo(126.977)
        assertThat(attraction.mapY).isEqualTo(37.578)
        assertThat(attraction.visible).isTrue
        assertThat(attraction.cat1).isEqualTo("A02")
    }

    @Test
    fun `showflag 0 은 visible=false 로 매핑된다`() {
        val item = AreaBasedSyncListItem(contentid = "1", title = "지워진곳", showflag = "0")

        val attraction = item.toAttraction()

        assertThat(attraction.visible).isFalse
    }
}
