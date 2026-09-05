package com.peakda.server.domain.seasonal.application

import com.peakda.server.infrastructure.external.kma.flower.response.FlowerDetail
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BloomObservationMapperTest {
    @Test
    fun `개화 전 관측일과 개화일과 만발일을 파싱한다`() {
        val command = detail(
            bfShotDate = "2026-03-25",
            cfShotDate = "2026-03-29",
            ffShotDate = "2026-04-02",
        ).toUpsertCommand()

        assertThat(command?.buddingOn).isEqualTo(LocalDate.of(2026, 3, 25))
        assertThat(command?.floweringOn).isEqualTo(LocalDate.of(2026, 3, 29))
        assertThat(command?.fullBloomOn).isEqualTo(LocalDate.of(2026, 4, 2))
    }

    @Test
    fun `관측 연도는 개화일에서 우선 취한다`() {
        val command = detail(
            bfShotDate = "2025-12-31",
            cfShotDate = "2026-03-29",
            ffShotDate = "2027-04-02",
        ).toUpsertCommand()

        assertThat(command?.obsYear).isEqualTo(2026)
    }

    @Test
    fun `개화일이 없으면 개화 전 관측일 연도로 떨어진다`() {
        val command = detail(
            bfShotDate = "2026-03-25",
            cfShotDate = null,
            ffShotDate = "2027-04-02",
        ).toUpsertCommand()

        assertThat(command?.obsYear).isEqualTo(2026)
    }

    @Test
    fun `관측 날짜가 전부 없으면 적재 명령을 만들지 않는다`() {
        val command = detail(
            bfShotDate = null,
            cfShotDate = null,
            ffShotDate = null,
        ).toUpsertCommand()

        assertThat(command).isNull()
    }

    private fun detail(
        bfShotDate: String?,
        cfShotDate: String?,
        ffShotDate: String?,
    ) = FlowerDetail(
        treeType = "벚나무",
        obsPlace = "여의도 윤중로",
        obsPlaceDetail = "영등포구 여의서로",
        flowerStatus = "3",
        bfShotDate = bfShotDate,
        cfShotDate = cfShotDate,
        ffShotDate = ffShotDate,
        modDate = "2026-04-02 18:10:00",
    )
}
