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

        assertThat(attraction.tourApiContentId).isEqualTo("126128")
        assertThat(attraction.title).isEqualTo("경복궁")
        assertThat(attraction.longitude).isEqualTo(126.977)
        assertThat(attraction.latitude).isEqualTo(37.578)
        assertThat(attraction.visible).isTrue
        assertThat(attraction.categoryMajor).isEqualTo("A02")
    }

    @Test
    fun `showflag 0 은 visible=false 로 매핑된다`() {
        val item = AreaBasedSyncListItem(contentid = "1", title = "지워진곳", showflag = "0")

        val attraction = item.toAttraction()

        assertThat(attraction.visible).isFalse
    }

    @Test
    fun `법정동 코드를 5자리 시군구 코드로 정규화해 명소와 upsert 명령 모두에 담는다`() {
        val item = AreaBasedSyncListItem(contentid = "1", title = "경복궁", lDongRegnCd = "11", lDongSignguCd = "110")

        val attraction = item.toAttraction()
        val command = item.toUpsertCommand()

        assertThat(attraction.legalDongAreaCode).isEqualTo("11")
        assertThat(attraction.legalDongSigunguCode).isEqualTo("11110")
        assertThat(command.legalDongAreaCode).isEqualTo("11")
        assertThat(command.legalDongSigunguCode).isEqualTo("11110")
    }

    @Test
    fun `갱신 응답에 법정동 코드가 비어 있으면 기존 값을 유지한다`() {
        val attraction = AreaBasedSyncListItem(contentid = "1", title = "경복궁", lDongRegnCd = "11", lDongSignguCd = "110")
            .toAttraction()

        attraction.applyUpdate(AreaBasedSyncListItem(contentid = "1", title = "경복궁"))

        assertThat(attraction.legalDongSigunguCode).isEqualTo("11110")
    }
}
