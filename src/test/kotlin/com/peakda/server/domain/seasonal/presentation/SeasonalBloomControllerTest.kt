package com.peakda.server.domain.seasonal.presentation

import com.peakda.server.domain.seasonal.application.BloomCalendarService
import com.peakda.server.domain.seasonal.application.BloomQueryService
import com.peakda.server.domain.seasonal.application.SpotBloomMapService
import com.peakda.server.domain.seasonal.presentation.response.BloomMapResponse
import com.peakda.server.domain.spot.presentation.response.PromotionalMapPinResponse
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class SeasonalBloomControllerTest {
    @Test
    fun `지도 응답은 상어 외부 링크와 트리 일출 내부 경로를 기존 홍보 핀 형식으로 제공한다`() {
        val mapService = mock(SpotBloomMapService::class.java)
        `when`(mapService.map(-1.0, 1.0, -1.0, 1.0, null, null, null, null)).thenReturn(
            BloomMapResponse(
                baseDate = null,
                count = 0,
                pins = emptyList(),
                promotionalPins = listOf(
                    PromotionalMapPinResponse(
                        id = "BUSAN_NORTH_PORT",
                        name = "부산 북항",
                        latitude = 0.0,
                        longitude = 0.0,
                        iconKey = "SHARK",
                        linkUrl = "https://example.com/north-port",
                    ),
                    PromotionalMapPinResponse("TREE_201", "테스트 트리", 0.1, 0.2, "TREE", "/festivals/201"),
                    PromotionalMapPinResponse("SUNRISE_202", "테스트 일출", 0.3, 0.4, "SUNRISE", "/festivals/202"),
                ),
            ),
        )
        val mvc = MockMvcBuilders.standaloneSetup(
            SeasonalBloomController(mock(BloomQueryService::class.java), mock(BloomCalendarService::class.java), mapService),
        ).build()

        mvc.perform(
            get("/api/seasonal/blooms").accept(MediaType.APPLICATION_JSON)
                .param("minLat", "-1.0").param("maxLat", "1.0")
                .param("minLng", "-1.0").param("maxLng", "1.0"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.data.count").value(0))
            .andExpect(jsonPath("$.data.pins").isEmpty)
            .andExpect(jsonPath("$.data.promotionalPins.length()").value(3))
            .andExpect(jsonPath("$.data.promotionalPins[0].id").value("BUSAN_NORTH_PORT"))
            .andExpect(jsonPath("$.data.promotionalPins[0].iconKey").value("SHARK"))
            .andExpect(jsonPath("$.data.promotionalPins[0].latitude").value(0.0))
            .andExpect(jsonPath("$.data.promotionalPins[0].longitude").value(0.0))
            .andExpect(jsonPath("$.data.promotionalPins[0].linkUrl").value("https://example.com/north-port"))
            .andExpect(jsonPath("$.data.promotionalPins[1].id").value("TREE_201"))
            .andExpect(jsonPath("$.data.promotionalPins[1].name").value("테스트 트리"))
            .andExpect(jsonPath("$.data.promotionalPins[1].latitude").value(0.1))
            .andExpect(jsonPath("$.data.promotionalPins[1].longitude").value(0.2))
            .andExpect(jsonPath("$.data.promotionalPins[1].iconKey").value("TREE"))
            .andExpect(jsonPath("$.data.promotionalPins[1].linkUrl").value("/festivals/201"))
            .andExpect(jsonPath("$.data.promotionalPins[2].id").value("SUNRISE_202"))
            .andExpect(jsonPath("$.data.promotionalPins[2].name").value("테스트 일출"))
            .andExpect(jsonPath("$.data.promotionalPins[2].latitude").value(0.3))
            .andExpect(jsonPath("$.data.promotionalPins[2].longitude").value(0.4))
            .andExpect(jsonPath("$.data.promotionalPins[2].iconKey").value("SUNRISE"))
            .andExpect(jsonPath("$.data.promotionalPins[2].linkUrl").value("/festivals/202"))
    }
}
