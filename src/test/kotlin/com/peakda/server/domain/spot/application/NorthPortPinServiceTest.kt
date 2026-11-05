package com.peakda.server.domain.spot.application

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class NorthPortPinServiceTest {
    private val service = NorthPortPinService(
        NorthPortPinProperties(
            enabled = true,
            latitude = 0.0,
            longitude = 0.0,
            linkUrl = "https://example.com/north-port?source=peakda",
        ),
    )

    @Test
    fun `비활성일 때 좌표와 링크 없이도 빈 목록을 반환한다`() {
        val disabled = NorthPortPinService(NorthPortPinProperties())
        assertThat(disabled.findInBoundingBox(-90.0, 90.0, -180.0, 180.0)).isEmpty()
    }

    @Test
    fun `bbox 경계 위 핀도 포함하고 설정된 링크를 그대로 반환한다`() {
        val pins = service.findInBoundingBox(0.0, 1.0, -1.0, 0.0)
        val pin = pins.single()
        assertThat(pin.latitude).isZero()
        assertThat(pin.longitude).isZero()
        assertThat(pin.linkUrl).isEqualTo("https://example.com/north-port?source=peakda")
    }

    @Test
    fun `위도 또는 경도가 영역 밖이면 핀을 반환하지 않는다`() {
        assertThat(service.findInBoundingBox(0.1, 1.0, -1.0, 1.0)).isEmpty()
        assertThat(service.findInBoundingBox(-1.0, -0.1, -1.0, 1.0)).isEmpty()
        assertThat(service.findInBoundingBox(-1.0, 1.0, 0.1, 1.0)).isEmpty()
        assertThat(service.findInBoundingBox(-1.0, 1.0, -1.0, -0.1)).isEmpty()
    }
}
