package com.peakda.server.infrastructure.external.kma

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GridConverterTest {
    private val converter = GridConverter()

    @Test
    fun `known city coordinates convert to KMA grid`() {
        assertThat(converter.toGrid(37.5665, 126.9780)).isEqualTo(GridCoordinate(60, 127))
        assertThat(converter.toGrid(35.1796, 129.0756)).isEqualTo(GridCoordinate(98, 76))
        assertThat(converter.toGrid(33.4996, 126.5312)).isEqualTo(GridCoordinate(53, 38))
    }

    @Test
    fun `grid coordinates convert back near original coordinate`() {
        val latLon = converter.toLatLon(60, 127)

        assertThat(latLon.latitude).isBetween(37.0, 38.0)
        assertThat(latLon.longitude).isBetween(126.0, 127.5)
    }
}
