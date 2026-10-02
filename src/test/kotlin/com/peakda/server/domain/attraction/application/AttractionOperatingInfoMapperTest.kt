package com.peakda.server.domain.attraction.application

import com.peakda.server.domain.attraction.repository.AttractionOperatingInfoTarget
import com.peakda.server.infrastructure.external.kto.korservice.response.DetailInfoItem
import com.peakda.server.infrastructure.external.kto.korservice.response.DetailIntroItem
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AttractionOperatingInfoMapperTest {

    private val target = AttractionOperatingInfoTarget(
        attractionId = 501L,
        tourApiContentId = "126128",
        contentTypeCode = "12",
        externalModifiedAt = "20260901120000",
    )

    @Test
    fun `소개정보의 이용시간·쉬는날·주차와 반복정보의 입장료를 합친다`() {
        val intro = DetailIntroItem(
            contentid = "126128",
            usetime = "09:00~18:00<br>※ 입장 마감 17:00",
            restdate = "매주 월요일",
            parking = "가능<br />요금 (최초 2시간 무료)",
        )
        val infos = listOf(
            DetailInfoItem(infoname = "화장실", infotext = "있음"),
            DetailInfoItem(infoname = "입 장 료", infotext = "어른 3,000원<br>어린이 1,000원"),
        )

        val command = target.toOperatingInfoUpsertCommand(intro, infos)

        assertThat(command.attractionId).isEqualTo(501L)
        assertThat(command.operatingHours).isEqualTo("09:00~18:00\n※ 입장 마감 17:00")
        assertThat(command.closedDays).isEqualTo("매주 월요일")
        assertThat(command.parking).isEqualTo("가능\n요금 (최초 2시간 무료)")
        assertThat(command.admissionFee).isEqualTo("어른 3,000원\n어린이 1,000원")
        assertThat(command.sourceModifiedAt).isEqualTo("20260901120000")
    }

    @Test
    fun `소개정보가 없고 입장료 반복정보도 없으면 모든 항목이 null 이다`() {
        val command = target.toOperatingInfoUpsertCommand(null, listOf(DetailInfoItem(infoname = "화장실", infotext = "있음")))

        assertThat(command.operatingHours).isNull()
        assertThat(command.closedDays).isNull()
        assertThat(command.admissionFee).isNull()
        assertThat(command.parking).isNull()
    }

    @Test
    fun `태그·HTML 엔티티·겹친 공백을 걷어내고 빈 줄은 지운다`() {
        assertThat(cleanTourText("<p>상시&nbsp;&nbsp;개방</p><br><br>  연중무휴 &amp; 무료 ")).isEqualTo("상시 개방\n연중무휴 & 무료")
        assertThat(cleanTourText("<br>  <br/>")).isNull()
        assertThat(cleanTourText("")).isNull()
        assertThat(cleanTourText(null)).isNull()
    }
}
